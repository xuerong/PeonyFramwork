package com.peony.engine.framework.control.job;

import com.peony.engine.framework.control.annotation.NetEventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.netEvent.NetEventData;
import com.peony.engine.framework.control.netEvent.NetEventService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.IdService;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.ServerType;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.util.Util;
import com.sun.corba.se.spi.ior.ObjectKey;
import jdk.nashorn.internal.scripts.JO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by apple on 16-9-4.
 *
 * TODO 目前先用ScheduledThreadPoolExecutor实现，后面可以考虑使用quartz实现
 * TODO 还没有考虑job的事务，即事务提交之后才能提交相应job...主要是加在jobExecutorMap中的如果事务失败要删除掉
 */
@Service(init = "init",initPriority = 4)
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    // 执行job的调度器,这个线程数不用处理器的个数,因为有些job会有数据库操作
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(100);
    private ConcurrentHashMap<Long,JobExecutor> jobExecutorMap = new ConcurrentHashMap<>();

    private NetEventService netEventService;
    private DataService dataService;
    private IdService idService;


    public void init(){
        List<Job> jobList = dataService.selectList(Job.class,"serverId=?",Server.getServerId());
        for (Job job : jobList){
            startJob(job,false);
        }
    }

    @Tx
    public long startJob(int delay,Class<?> service, String method,Object... params){
        Job job = new Job();
        job.setId(idService.acquireLong(Job.class));
        job.setServerId(Server.getServerId());
        job.setStartDate(new Timestamp(System.currentTimeMillis()+delay));
        job.setMethod(method);
        job.setServiceClass(service.getName());
        job.setParams(params);
        startJob(job);
        return job.getId();
    }

    private void startJob(Job job){
        startJob(job,true);
    }



    private void startJob(Job job,boolean isDb){
        JobExecutor jobExecutor = createJobExecutor(job);
        synchronized (jobExecutor) {
            // 本地验证唯一性
            JobExecutor oldJ = jobExecutorMap.putIfAbsent(job.getId(), jobExecutor);
            if (oldJ != null) { // 已经存在了
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
    @Tx
    public void deleteJob(long id){
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
                if((job.getParams() == null || job.getParams().length == 0)
                        &&(classes == null || classes.length == 0)){ // 都没有
                    method = methods[i];
                    break;
                }
                if((job.getParams() == null || job.getParams().length == 0)
                        ||(classes == null || classes.length == 0)){ // 其中一个没有
                    continue;
                }
                if(classes.length != job.getParams().length){// TODO 这个地方如何考虑Object...参数
                    continue;
                }
                // 比较参数
                Class<?>[] paraClasses = new Class[job.getParams().length];
                for(int p=0;p<paraClasses.length;p++){
                    paraClasses[p] = job.getParams()[p].getClass();
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
        jobExecutor.para = job.getParams();
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
            Date now = new Date();
            try {
                method.invoke(object,para);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            deleteJob(id,true);
        }
    }
}
