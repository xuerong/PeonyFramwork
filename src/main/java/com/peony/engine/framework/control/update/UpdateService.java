package com.peony.engine.framework.control.update;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.annotation.Updatable;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.control.update.cronExpression.CronExpression;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.ServerType;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.thread.ThreadPoolHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by Administrator on 2015/11/23.
 *
 * 更新器，完成对所有更新服务的更新功能，如下：
 * 1对于同步更新器，每隔固定周期就会更新一次，更新周期由系统配置文件设置，如果更新内容耗时小于更新周期，将通过等待维持更新周期，如果更新内容耗时大于更新周期
 * 更新周期将被迫降低，出现这种情况，说明需要适当的减少更新内容耗时或增加更新周期
 * 2对于异步更新器，将提供线程池进行更新
 */
@Service(init = "init",destroy = "destroy")
public class UpdateService {

    private List<UpdatableBean> asyncUpdatableList=new ArrayList<>();
//    private List<UpdatableBean> syncUpdatableList=new ArrayList<>();

    // 线程数量可以是处理器数量*2+1：Runtime.getRuntime().availableProcessors()
    // 线程池这里最好也重写，给线程命名标记
    private ScheduledExecutorService asyncExecutor = ThreadPoolHelper.newScheduledThreadPoolExecutor("Update",16);

    public void init(){
        Map<Class<?>,List<Method>> updatableClassMap= ServiceHelper.getUpdatableClassMap();
        for(Map.Entry<Class<?>,List<Method>> entry : updatableClassMap.entrySet()){
            Object service= BeanHelper.getServiceBean(entry.getKey());
            List<Method> methodList=entry.getValue();
            for(Method method : methodList){
                method.setAccessible(true);//TODO 取消 Java 语言访问检查   看看其他地方能否用到
                Updatable updatable=method.getAnnotation(Updatable.class);// 前面ServiceHelper已经进行了校验此处不用重复校验
                UpdatableBean updatableBean=new UpdatableBean(service,method,updatable.cycle(),
                        updatable.cronExpression(),updatable.doOnStart());
                asyncUpdatableList.add(updatableBean);
//                if(updatable.isAsynchronous()){
//                    asyncUpdatableList.add(updatableBean);
//                }else {
//                    syncUpdatableList.add(updatableBean);
//                }
            }
        }
    }

    @EventListener(event = SysConstantDefine.Event_ServerStartAsync)
    public void serverStart(Object object){
        // 启动
        start();
    }
    public void destroy(){
        stop();
    }

    public void stop(){
//        syncExecutor.shutdown();
        asyncExecutor.shutdown();
    }

    public void start(){
        // 启动同步更新器
//        for(UpdatableBean updatableBean : syncUpdatableList){
//            updatableBean.lastUpdateTime=System.nanoTime();
//        }
        for(UpdatableBean updatableBean : asyncUpdatableList){
            updatableBean.lastUpdateTime=System.nanoTime();
        }

//        syncExecutor.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                //lastUpdateTime.
//                for(UpdatableBean updatableBean : syncUpdatableList){
//                    if(!updatableBean.runEveryServer && !ServerType.isMainServer()){ // TODO 后面改成分发给nodeServer
//                        continue;
//                    }
//                    updatableBean.execute();
//                }
//            }
//        },syncUpdateInterval,syncUpdateInterval,TimeUnit.MILLISECONDS);
        // 启动异步更新器
        for(final UpdatableBean updatableBean : asyncUpdatableList){
            if(updatableBean.getCronExpression()==null) { // 纯周期运行
                asyncExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        updatableBean.execute();
                    }
                }, updatableBean.isDoOnStart()?0:updatableBean.getInterval(), updatableBean.getInterval(), TimeUnit.MILLISECONDS);
            }else{ // 通过cronExpression来生成运行时间点的周期运行
                // cronExpression
                Date now = new Date();
                Date time = updatableBean.getCronExpression().getNextValidTimeAfter(now);
                long delay = time.getTime() - now.getTime();
                asyncExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        updatableBean.execute();
                        // ----
                        Date now = new Date();
                        Date time = updatableBean.cronExpression.getNextValidTimeAfter(now);
                        long delay = time.getTime() - now.getTime();
                        asyncExecutor.schedule(this,delay,TimeUnit.MILLISECONDS);
                    }
                },delay,TimeUnit.MILLISECONDS);
            }
        }
    }

    private class UpdatableBean{
//        private boolean isAsynchronous;
        private int interval;
        private long lastUpdateTime;
//        private boolean runEveryServer;

        private Object service;
        private Method method;

        private CronExpression cronExpression;
        private boolean doOnStart;

        private UpdatableBean(Object service,Method method,int interval,
                             String cronExpression,boolean doOnStart){
            this.service=service;
            this.method=method;
//            this.isAsynchronous=isAsynchronous;
            this.interval=interval;
//            this.runEveryServer = runEveryServer;
            this.doOnStart = doOnStart;
            if(cronExpression != null && cronExpression.length()>0) {
                try {
                    this.cronExpression = new CronExpression(cronExpression);
                } catch (ParseException e) {
                    throw new MMException(e);
                }
            }
        }
        // 这里必须捕获异常，防止阻塞其它更新器
        private void execute(){
            try {
                long currentTime=System.nanoTime();
                method.invoke(service,(int)((currentTime-lastUpdateTime)/1000000));
                lastUpdateTime=currentTime;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

//        public boolean isAsynchronous() {
//            return isAsynchronous;
//        }

        public int getInterval() {
            return interval;
        }

        public Object getService() {
            return service;
        }

        public Method getMethod() {
            return method;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public CronExpression getCronExpression() {
            return cronExpression;
        }

        public boolean isDoOnStart() {
            return doOnStart;
        }
    }
}
