package com.peony.engine.framework.control.event;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.NetEventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.netEvent.NetEventData;
import com.peony.engine.framework.control.netEvent.NetEventService;
import com.peony.engine.framework.data.tx.TxCacheService;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.procedure.TShortObjectProcedure;
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
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final int poolWarningSize=20;
    private static final int queueWarningSize=1000;

    private ThreadLocal<List<EventData>> cacheDatas = new ThreadLocal<>();


    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            10,100,3000, TimeUnit.MILLISECONDS,new LinkedBlockingDeque<Runnable>(),
            new RejectedExecutionHandler(){
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 拒绝执行
        }
    });


    private final TShortObjectHashMap<Set<EventListenerHandler>> handlerMap=new TShortObjectHashMap<>();
    private final TShortObjectHashMap<Set<EventListenerHandler>> synHandlerMap=new TShortObjectHashMap<>();

    private NetEventService netEventService;
    private TxCacheService txCacheService;

    public void init(){
        TShortObjectHashMap<Set<Class<?>>> handlerClassMap= ServiceHelper.getEventListenerHandlerClassMap();
        handlerClassMap.forEachEntry(new TShortObjectProcedure<Set<Class<?>>>() {
            @Override
            public boolean execute(short i, Set<Class<?>> classes) {
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

        TShortObjectHashMap<Set<Class<?>>> synHandlerClassMap= ServiceHelper.getEventSynListenerHandlerClassMap();
        synHandlerClassMap.forEachEntry(new TShortObjectProcedure<Set<Class<?>>>() {
            @Override
            public boolean execute(short i, Set<Class<?>> classes) {
                Set<EventListenerHandler> handlerSet=new HashSet<EventListenerHandler>();
                for(Class<?> cls : classes){
                    handlerSet.add((EventListenerHandler)BeanHelper.getServiceBean(cls));
                }
                synHandlerMap.put(i,handlerSet);
                return true;
            }
        });
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

    public void txCommitFinish(boolean success){
        List<EventData> list = cacheDatas.get();
        if(list == null){
            return;
        }
        cacheDatas.remove();
        if(!success){
            return;
        }
        for(EventData eventData:list){
            if(eventData == null){
                continue;
            }
            doASyncEvent(eventData,false);
        }
    }

    AtomicInteger test = new AtomicInteger();
    // 事件事务只存在于异步事件中，同步事件执行原来就在事务中，
    private boolean checkAndAddTx(EventData eventData){
        if(txCacheService.isInTx()){
            test.getAndIncrement();
            List<EventData> list = cacheDatas.get();
            if(list == null){
                list = new ArrayList<>();
                cacheDatas.set(list);
            }
            list.add(eventData);
            return true;
        }
        return false;
    }

    /**
     *
     */
    public void fireAll(Object data, int event){
        fireAll(data,event,false);
    }
    /**
     *
     */
    public void fireAll(Object data, int event, final boolean broadcast){
        EventData eventData = new EventData(event);
        eventData.setData(data);

        // 同步的
        doSyncEvent(eventData,broadcast);
        // 异步的
        doASyncEvent(eventData,broadcast);

        // TODO 这里没有考虑同步异步的问题，后续要加上
        if(broadcast){
            NetEventData netEventData = new NetEventData(SysConstantDefine.broadcastEvent);
            netEventData.setParam(eventData);
            netEventService.broadcastNetEvent(netEventData,false);
        }
    }

    private void doSyncEvent(EventData eventData,boolean broadcast){
        Set<EventListenerHandler> synHandlerSet = synHandlerMap.get(eventData.getEvent());
        if(synHandlerSet != null && synHandlerSet.size() > 0){
            for (EventListenerHandler handler : synHandlerSet) {
                try {
                    handler.handleSyn(eventData);
                }catch (Throwable e){
                    log.error("exception happened while syn fire event :"+eventData.getEvent()+",handler in:"+handler.getClass(),e);
                }
            }
        }
    }
    private void doASyncEvent(EventData eventData,boolean broadcast){
        final Set<EventListenerHandler> handlerSet = handlerMap.get(eventData.getEvent());
        if(handlerSet != null && handlerSet.size() > 0) {
            if(checkAndAddTx(eventData)){
                return;
            }
            executor.execute(() -> {
                for (EventListenerHandler handler : handlerSet) {
                    try {
                        handler.handle(eventData);
                    } catch (Throwable e) {
                        log.error("exception happened while fire event :" + eventData.getEvent() + ",handler in:" + handler.getClass(), e);
                    }
                }
            });
        }
    }


    /**
     * 事件是异步的
     * TODO 发出事件改为四种:同步|异步*广播|不广播
     * **/

    public void fireEvent(Object data, int event){
        fireAll(data,event);
    }

    // 接受到其它服务器发送的事件
    @NetEventListener(netEvent = SysConstantDefine.broadcastEvent)
    public NetEventData receiveEventData(NetEventData netEventData){
        EventData eventData = (EventData)netEventData.getParam();
        doASyncEvent(eventData,false); // 这个地方分为同步和异步
        return null;
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
        EventData eventData = new EventData(event);
        eventData.setData(data);

        if(handlerSet1 != null){
            for (EventListenerHandler handler : handlerSet1) {
                try {
                    handler.handle(eventData);
                }catch (Throwable e){
                    log.error("exception happened while fire event :"+eventData.getEvent(),e);
                }
            }
        }
        if(handlerSet2 != null){
            for (EventListenerHandler handler : handlerSet2) {
                try {
                    handler.handle(eventData);
                }catch (Throwable e){
                    log.error("exception happened while fire event :"+eventData.getEvent(),e);
                }
            }
        }
    }
}
