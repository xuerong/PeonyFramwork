package com.peony.engine.framework.control.event;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.tx.AbListDataTxLifeDepend;
import com.peony.engine.framework.data.tx.ITxLifeDepend;
import com.peony.engine.framework.data.tx.TxCacheService;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.thread.ThreadPoolHelper;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2015/11/20.
 *
 * 事件分为同步执行和异步执行，
 * 同步执行的：事件监听者完成处理才返回：需要监控
 * 一步执行的：通过线程池分配线程执行触发各个事件，确保两点：
 * 1、事件线程有最大值，超过最大值，事件要排队
 * 2、事件队列有最大值，超过最大值，抛出服务器异常：可在某个比较大的值抛出警告
 */
@Service(init = "init",initPriority = 1)
public class EventService{
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final int poolWarningSize=20;
    private static final int queueWarningSize=1000;

    private ThreadLocal<List<EventData>> cacheDatas = new ThreadLocal<>();
    EventTxLifeDepend eventTxLifeDepend = new EventTxLifeDepend();

    private final ThreadPoolExecutor executor = ThreadPoolHelper.newThreadPoolExecutor("PpeonyEvent",16,1024,1024);


    private final TIntObjectHashMap<Set<EventListenerHandler>> handlerMap=new TIntObjectHashMap<>();
    private final TIntObjectHashMap<Set<EventListenerHandler>> synHandlerMap=new TIntObjectHashMap<>();

    private TxCacheService txCacheService;

    public void init(){
        TIntObjectHashMap<Set<Class<?>>> handlerClassMap= ServiceHelper.getEventListenerHandlerClassMap();
        handlerClassMap.forEachEntry(new TIntObjectProcedure<Set<Class<?>>>() {
            @Override
            public boolean execute(int i, Set<Class<?>> classes) {
                if(classes.size() > 0) {
                    Set<EventListenerHandler> handlerSet = new HashSet<EventListenerHandler>();
                    for (Class<?> cls : classes) {
                        handlerSet.add((EventListenerHandler) BeanHelper.getServiceBean(cls));
                    }
                    handlerMap.put(i, handlerSet);
                }
                return true;
            }
        });

        TIntObjectHashMap<Set<Class<?>>> synHandlerClassMap= ServiceHelper.getEventSynListenerHandlerClassMap();
        synHandlerClassMap.forEachEntry(new TIntObjectProcedure<Set<Class<?>>>() {
            @Override
            public boolean execute(int i, Set<Class<?>> classes) {
                Set<EventListenerHandler> handlerSet=new HashSet<EventListenerHandler>();
                for(Class<?> cls : classes){
                    handlerSet.add((EventListenerHandler)BeanHelper.getServiceBean(cls));
                }
                synHandlerMap.put(i,handlerSet);
                return true;
            }
        });
        //
        txCacheService.registerTxLifeDepend(eventTxLifeDepend);
    }

    // 最后用一个系统的检测服务update进行系统所有的监测任务
    public String getMonitorData(){
        BlockingQueue<Runnable> queue = executor.getQueue();
        int poolSize=executor.getPoolSize();
        if(poolSize>poolWarningSize || queue.size()>queueWarningSize){
            return "event thread executor pool is too big poolSize:"+poolSize +",queueSize:"+queue.size();
        }
        return "ok";
    }

    class EventTxLifeDepend extends AbListDataTxLifeDepend{
        @Override
        protected void executeTxCommit(Object object) {
            EventData eventData = (EventData)object;
            doASyncEvent(eventData.getEvent(),eventData.getData());
        }
    }

    // 事件事务只存在于异步事件中，同步事件执行原来就在事务中，
    private boolean checkAndAddTx(int event,Object data){
        EventData eventData = new EventData(event);
        eventData.setData(data);
        return eventTxLifeDepend.checkAndPut(eventData);
    }



    private void doSyncEvent(int event,Object data){
        Set<EventListenerHandler> synHandlerSet = synHandlerMap.get(event);
        if(synHandlerSet != null && synHandlerSet.size() > 0){
            for (EventListenerHandler handler : synHandlerSet) {
                try {
                    handler.handleSyn(event,data);
                }catch (Throwable e){
                    log.error("exception happened while syn fire event :"+event+",handler in:"+handler.getClass(),e);
                }
            }
        }
    }
    private void doASyncEvent(int event,Object data){
        final Set<EventListenerHandler> handlerSet = handlerMap.get(event);
        if(handlerSet != null && handlerSet.size() > 0) {
            if(checkAndAddTx(event, data)){
                return;
            }
            executor.execute(() -> {
                for (EventListenerHandler handler : handlerSet) {
                    try {
                        handler.handle(event, data);
                    } catch (Throwable e) {
                        log.error("exception happened while fire event :" + event + ",handler in:" + handler.getClass(), e);
                    }
                }
            });
        }
    }


    /**
     * 事件是异步的
     * 发出事件改为两种:同步|异步
     * @param data 事件参数
     * @param event 事件id
     */
    public void fireEvent(Object data, int event){
//        EventData eventData = new EventData(event);
//        eventData.setData(data);

        // 同步的
        doSyncEvent(event, data);
        // 异步的
        doASyncEvent(event, data);
    }

    /**
     * 同步触发事件，即事件完成方可返回，不管事件本身设置了同步还是异步
     * */
    public void fireEventSyn(Object data,int event){

        Set<EventListenerHandler> handlerSet1 = handlerMap.get((short) event);
        Set<EventListenerHandler> handlerSet2 = synHandlerMap.get((short)event);

        if(handlerSet1 == null && handlerSet2 == null){
            return;
        }
//        EventData eventData = new EventData(event);
//        eventData.setData(data);

        checkAndHandle(handlerSet1,event,data,false);
        checkAndHandle(handlerSet2,event,data,true);
    }

    private void checkAndHandle(Set<EventListenerHandler> handlerSet1,int event,Object data,boolean syncMethod){
        if(handlerSet1 != null){
            for (EventListenerHandler handler : handlerSet1) {
                try {
                    if(syncMethod){
                        handler.handleSyn(event, data);
                    }else{
                        handler.handle(event, data);
                    }

                }catch (Throwable e){
                    log.error("exception happened while fire event :"+event,e);
                }
            }
        }
    }
}
