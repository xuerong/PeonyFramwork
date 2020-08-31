package com.peony.core.control.job;

import com.peony.core.control.annotation.Service;
import com.peony.core.data.DataService;
import com.peony.core.data.tx.AbListDataTxLifeDepend;
import com.peony.core.data.tx.Tx;
import com.peony.core.data.tx.TxCacheService;
import com.peony.common.exception.MMException;
import com.peony.core.server.IdService;
import com.peony.core.server.Server;
import com.peony.core.control.BeanHelper;
import com.peony.common.tool.thread.ThreadPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.*;

/**
 * Job服务。Job指过一段时间后执行某个方法。
 * <p>
 * Job使用的是{@code ScheduledThreadPoolExecutor}实现。同时Job会保存在数据库中，确保
 * Job不会因为服务器关闭而被清除。
 * <p>
 * TODO 目前先用ScheduledThreadPoolExecutor实现，后面可以考虑使用quartz实现
 *
 * @author zhengyuzhen
 * @see Job
 * @since 1.0
 */
@Service(init = "init",initPriority = 4)
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    // 执行job的调度器
    private ScheduledThreadPoolExecutor executor = ThreadPoolHelper.newScheduledThreadPoolExecutor("Job",16);
    // 未执行的job
    private ConcurrentHashMap<Long,JobExecutor> jobExecutorMap = new ConcurrentHashMap<>();
    // 事务依赖处理器
    private JobTxLifeDepend<Job> jobTxLifeDepend = new JobTxLifeDepend<>();

    private DataService dataService;
    private IdService idService;
    private TxCacheService txCacheService;


    public void init(){
        // 系统初始化时，从数据库中取出之前的Job，并启动
        List<Job> jobList = dataService.selectList(Job.class,"serverId=?",Server.getServerId());
        for (Job job : jobList){
            startJob(job,false);
        }
        // 注册事务依赖处理器
        txCacheService.registerTxLifeDepend(jobTxLifeDepend);
    }

    /**
     * 启动一个Job
     *
     * @param delay 延时时间
     * @param service Service类，必须是@Service
     * @param method 方法名
     * @param params 方法的参数
     * @return Job的id
     */
    @Tx
    public long startJob(int delay,Class<?> service, String method,Object... params){
        Job job = new Job();
        job.setId(idService.acquireLong(Job.class));
        job.setServerId(Server.getServerId());
        job.setStartDate(new Timestamp(System.currentTimeMillis()+delay));
        job.setMethod(method);
        job.setServiceClass(service.getName());
        job.setParamsObjectArray(params);
        startJob(job);
        return job.getId();
    }

    private void startJob(Job job){
        // 如果在事务中，先放入事务
        if(jobTxLifeDepend.checkAndPut(job)){
            return;
        }
        startJob(job,true);
    }

    private void startJob(Job job,boolean isDb){
        JobExecutor jobExecutor = createJobExecutor(job);
        synchronized (jobExecutor){
            // 本地验证唯一性
            JobExecutor oldJ = jobExecutorMap.putIfAbsent(job.getId(), jobExecutor);
            if (oldJ != null) { // 已经存在了
                log.error("job id error!");
                throw new MMException("job has exist! id = " + job.getId());
            }
            // 存储
            if(isDb){
                dataService.insert(job);
            }
            // 实际运行
            RunnableScheduledFuture<?> future = (RunnableScheduledFuture) executor.schedule(jobExecutor,
                    jobExecutor.delay, TimeUnit.MILLISECONDS); // 这里delay<0是处理的了
            //
            jobExecutor.future = future;
        }
    }

    /**
     * 事务依赖器，事务提交时才启动事务
     *
     * @param <T>
     */
    class JobTxLifeDepend<T> extends AbListDataTxLifeDepend<T> {
        @Override
        protected void executeTxCommit(T object) {
            startJob((Job) object);
        }

        public boolean checkAndDelete(long id){
            if(txCacheService.isInTx()){
                List<T> jobList = jobThreadLocal.get();
                if(jobList != null){
                    for(T t : jobList){
                        Job job = (Job)t;
                        if(job.getId() == id){
                            jobList.remove(t);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }


    /**
     * 删除一个Job
     *
     * @param id job的id
     */
    @Tx
    public void deleteJob(long id){
        // 如果在事务中，先尝试从事务中删除
        if(jobTxLifeDepend.checkAndDelete(id)){
            return;
        }
        deleteJob(id,false);
    }

    private void deleteJob(long id,boolean jobFinish){
        // 清除自身的保存
        JobExecutor jobExecutor = jobExecutorMap.remove(id);
        if(jobExecutor != null){
            synchronized (jobExecutor) {
                if(!jobFinish) {
                    jobExecutor.future.cancel(true);
                }
                dataService.delete(Job.class,"id=?",id);
            }
        }
    }

    /**
     * 创建Job执行器
     *
     * @param job
     * @return
     */
    private JobExecutor createJobExecutor(Job job){
        JobExecutor jobExecutor = new JobExecutor();
        jobExecutor.id = job.getId();

        long delay = job.getStartDate().getTime() - System.currentTimeMillis();
        jobExecutor.delay = delay>0?delay:0;
        Object bean = BeanHelper.getServiceBean(job.getServiceClass());
        if(bean == null){
            throw new MMException("job set error: service is not exist:"+job.getServiceClass());
        }
        Method method = null;

        Method[] methods = bean.getClass().getMethods();
        for(int i = 0;i<methods.length;i++){
            if(methods[i].getName().equals(job.getMethod())){
                Class<?>[] classes = methods[i].getParameterTypes(); // 它是可能是父类
                if((job.getParamsObjectArray() == null || job.getParamsObjectArray().length == 0)
                        &&(classes == null || classes.length == 0)){ // 都没有
                    method = methods[i];
                    break;
                }
                if((job.getParamsObjectArray() == null || job.getParamsObjectArray().length == 0)
                        ||(classes == null || classes.length == 0)){ // 其中一个没有
                    continue;
                }
                if(classes.length != job.getParamsObjectArray().length){//
                    continue;
                }
                // 比较参数
                Class<?>[] paraClasses = new Class[job.getParamsObjectArray().length];
                for(int p=0;p<paraClasses.length;p++){
                    paraClasses[p] = job.getParamsObjectArray()[p].getClass();
                }
                boolean success = true;
                for(int k = 0;k<classes.length;k++){
                    if(!classes[k].isAssignableFrom(paraClasses[k])){
                        success = false;
                        break;
                    }
                }
                if(success) {
                    method = methods[i];
                    break;
                }
            }
        }
        if(method == null){
            throw new MMException("can't find method with such para: "+job.getMethod());
        }
        jobExecutor.method = method;
        jobExecutor.para = job.getParamsObjectArray();
        jobExecutor.object = bean;

        return jobExecutor;
    }

    private class JobExecutor implements Runnable{
        private long id;
        //
        private long delay; // 第一次执行时间,

        private Method method;
        private Object object;

        private Object[] para;

        private RunnableScheduledFuture<?> future;

        @Override
        public void run() {
            try {
                method.invoke(object,para);
            } catch (IllegalAccessException e) {
                log.error("job execute error!",e);
            } catch (InvocationTargetException e) {
                log.error("job execute error!",e);
            }finally {
                deleteJob(id,true);
            }
        }
    }
}
