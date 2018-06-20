package com.peony.engine.framework.control.job;

import com.peony.engine.framework.control.annotation.NetEventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.netEvent.NetEventData;
import com.peony.engine.framework.control.netEvent.NetEventService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.ServerType;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by apple on 16-9-4.
 * job分为两种:
 * 一种是指执行一次:用时间段表示
 *
 * job实际运行在各个服务器上,mainServer负责调度job的id,防止重复
 *
 * TODO 目前先用ScheduledThreadPoolExecutor实现，后面可以考虑使用quartz实现
 * TODO 还没有考虑job的事务，即事务提交之后才能提交相应job
 * TODO job是否放入数据库呢？我偏向于不放在数据库，原因如下：
 * 1 job的存储是系统的事情，数据库只存储和游戏逻辑相关的东西
 * 2 job在系统启动的时候载入即可，不需要走缓存等
 * ....是采取随时保存，还是系统关闭的时候保存（不行，回滚都来不及），还是随时异步保存？
 *
 *
 * job在启动的时候遇到一些问题：
 * 1 先启动的服务器把所有的job给加载了
 * 2 db的job获取之后如何分发？主要是服务器启动有顺序性，况且原来创建该job的服务器未必会启动
 */
@Service(init = "init",initPriority = 4)
public class JobService {
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    // 执行job的调度器,这个线程数不用处理器的个数,因为有些job会有数据库操作
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(100);
    private ConcurrentHashMap<String,JobExecutor> jobExecutorMap = new ConcurrentHashMap<>();
    // 这里面存储的是所有的job的key,用来确保不能有重复的key,这个只在mainServer上面有效
    private ConcurrentHashMap<String,String> jobIds = new ConcurrentHashMap<>();

    private JobStorage jobStorage;

    private NetEventService netEventService;

    public void startJob(Job job){
        startJob(job,true);
    }

    public void deleteJob(String id){
        deleteJob(id,false);
    }
    public void init(){

        netEventService = BeanHelper.getServiceBean(NetEventService.class);

        if(ServerType.isMainServer()){ // TODO 后面可以改成分发给其它服务器，或者直接设置个job服务器，用于执行一些job
            // job是否放入数据库呢？我偏向于不放在数据库，原因如下：
            jobStorage = BeanHelper.getFrameBean(JobStorage.class);
            if(jobStorage != null){
                List<Job> jobList = jobStorage.getJobList();
                if(jobList != null) {
                    for (Job job : jobList) {
                        startJob(job, false);
                    }
                }
            }else{
                log.warn("jobStorage == null");
            }
        }
    }
    @NetEventListener(netEvent = SysConstantDefine.checkJobId)
    public NetEventData checkJobId(NetEventData eventData){
        JobNetEventData data = (JobNetEventData)eventData.getParam();
        if(data.type == 1){ // 1 添加,2 移除
            String oldAdd = jobIds.putIfAbsent(data.id,data.serverAdd);
            eventData.setParam(oldAdd == null);
            return eventData;
        }else if(data.type == 2){ //移除
            String oldAdd = jobIds.remove(data.id);
            if(oldAdd == null ){
                return eventData;
            }
            if(!data.serverAdd.equals(oldAdd)){// 需要删除实际的job
                netEventService.fireServerNetEvent(oldAdd,new NetEventData(SysConstantDefine.removeJobOnServer,data.id));
            }
        }
        throw new MMException("netEvent error : "+eventData.getNetEvent());
    }
    @NetEventListener(netEvent =  SysConstantDefine.removeJobOnServer)
    public NetEventData removeJobOnServer(NetEventData data){
        String id = (String)data.getParam();
        JobExecutor jobExecutor = jobExecutorMap.remove(id);
        if(jobExecutor != null){
            synchronized (jobExecutor) {
                executor.remove(jobExecutor.future);
            }
        }
        data.setParam(Boolean.TRUE);
        return data;
    }
    private void startJob(Job job,boolean isDb){
        JobExecutor jobExecutor = createJobExecutor(job);
        synchronized (jobExecutor) {
            // 本地验证唯一性
            JobExecutor oldJ = jobExecutorMap.putIfAbsent(job.getId(), jobExecutor);
            if (oldJ != null) { // 已经存在了
                throw new MMException("job has exist! id = " + job.getId());
            }
            // 向远端注册唯一性
            JobNetEventData data = new JobNetEventData();
            data.id = job.getId();
            data.type = 1;
            data.serverAdd = Util.getHostAddress() + ":" + Server.getEngineConfigure().getNetEventPort();
            NetEventData result = netEventService.fireMainServerNetEventSyn(
                    new NetEventData(SysConstantDefine.checkJobId, data));
            if (!(boolean) (result.getParam())) { // 添加失败
                // 在本地删除
                jobExecutorMap.remove(job.getId());
                throw new MMException("job has exist! id = " + job.getId());
            }
            if(isDb && jobStorage!=null && job.isDb()){
                jobStorage.saveJob(job);
            }
            // 实际运行
            RunnableScheduledFuture<?> future = (RunnableScheduledFuture) executor.schedule(jobExecutor,
                    jobExecutor.delay, TimeUnit.MILLISECONDS); // 这里delay<0是处理的了
            // TODO
            jobExecutor.future = future;
        }
    }
    private void deleteJob(String id,boolean jobFinish){
        JobNetEventData data = new JobNetEventData();
        data.id = id;
        data.type = 2; // 移除
        data.serverAdd = Util.getHostAddress()+":"+ Server.getEngineConfigure().getNetEventPort();
        // 清楚同步id
        netEventService.fireMainServerNetEvent(
                new NetEventData(SysConstantDefine.checkJobId,data));
        // 清除自身的保存
        JobExecutor jobExecutor = jobExecutorMap.remove(id);
        if(jobExecutor != null){
            synchronized (jobExecutor) {
                if(!jobFinish) {
                    executor.remove(jobExecutor.future);
                }
                if(jobStorage != null && jobExecutor.db){
                    jobStorage.deleteJob(id);
                }
            }
        }
    }
    private JobExecutor createJobExecutor(Job job){
        JobExecutor jobExecutor = new JobExecutor();
        jobExecutor.id = job.getId();
        if(jobExecutor.id == null || jobExecutor.id.length() == 0){
            throw new MMException("job set error: id is null");
        }
        long delay = job.getStartDate().getTime() - System.currentTimeMillis();
        jobExecutor.delay = delay>0?delay:0;
        jobExecutor.db = job.isDb();
        Object bean = BeanHelper.getServiceBean(job.getServiceClass());
        if(bean == null){
            throw new MMException("job set error: service is not exist:"+job.getServiceClass().getName());
        }
        Method method = null;

        Method[] methods = job.getServiceClass().getMethods();
        for(int i = 0;i<methods.length;i++){
            if(methods[i].getName().equals(job.getMethod())){
                Class<?>[] classes = methods[i].getParameterTypes(); // 它是可能是父类
                if((job.getPara() == null || job.getPara().length == 0)
                        &&(classes == null || classes.length == 0)){ // 都没有
                    method = methods[i];
                    break;
                }
                if((job.getPara() == null || job.getPara().length == 0)
                        ||(classes == null || classes.length == 0)){ // 其中一个没有
                    continue;
                }
                if(classes.length != job.getPara().length){// TODO 这个地方如何考虑Object...参数
                    continue;
                }
                // 比较参数
                Class<?>[] paraClasses = new Class[job.getPara().length];
                for(int p=0;p<paraClasses.length;p++){
                    paraClasses[p] = job.getPara()[p].getClass();
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
        jobExecutor.para = job.getPara();
        jobExecutor.object = bean;

        return jobExecutor;
    }

    public class JobNetEventData implements Serializable{
        private int type; // 1 添加,2 移除

        private String id;
// 这个不要了,因为在逻辑中如果需要这样做,一定知道原来的job,就可以通过删除解决,否则,不能这样做
//        private boolean replaceIfExist;

        private String serverAdd; // server的地址
    }

    public class JobExecutor implements Runnable{
        private String id;
        //
        private long delay; // 第一次执行时间,

        private boolean db; // 是否持久化

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
