package com.peony.engine.framework.control.event;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.tx.AbListDataTxLifeDepend;
import com.peony.engine.framework.data.tx.TxCacheService;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.thread.ThreadPoolHelper;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 事件服务。
 * <p>
 * 通过事件服务抛出事件，通过{@link EventListener}监听事件。事件分为同步事件和异步事件。
 * 同步事件是指，事件监听函数的执行由抛出时同步执行，异步事件是事件监听函数在抛出之后，异步
 * 执行。
 * <p>
 * 事件的同步和异步，有两个地方可以设置，分别是事件同步/异步监听和事件同步/普通抛出：
 * <table>
 *     <tr><td></td><td>&nbsp;同步监听&nbsp;</td><td>&nbsp;异步监听&nbsp;</td></tr>
 *     <tr><td>同步抛出</td><td>&nbsp;同步执行</td><td>&nbsp;同步执行</td></tr>
 *     <tr><td>普通抛出</td><td>&nbsp;同步执行</td><td>&nbsp;异步执行</td></tr>
 * </table>
 *
 * @author zhengyuzhen
 * @see EventListener
 * @see EventListenerHandler
 * @since 1.0
 */
@Service(init = "init",initPriority = 1)
public class EventService{
    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    /**
     * 线程池的大小提醒限制
     */
    private static final int poolWarningSize=20;
    /**
     * 队列大小的提醒限制
     */
    private static final int queueWarningSize=1000;
    /**
     * 事务周期依赖，用于实现事件的事务依赖
     */
    EventTxLifeDepend eventTxLifeDepend = new EventTxLifeDepend();
    /**
     * 执行异步事件的线程池
     */
    private final ThreadPoolExecutor executor = ThreadPoolHelper.newThreadPoolExecutor("PpeonyEvent",16,1024,1024);

    /**
     * 普通事件处理器的缓存
     */
    private final TIntObjectHashMap<Set<EventListenerHandler>> handlerMap=new TIntObjectHashMap<>();
    /**
     * 同步事件处理器的缓存
     */
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
        // 注册事务周期依赖
        txCacheService.registerTxLifeDepend(eventTxLifeDepend);
    }

    /**
     * 事件的事务周期依赖，用于在事件抛出时，对于异步的事件，需要在事务提交成功之后才能执行
     */
    class EventTxLifeDepend extends AbListDataTxLifeDepend{
        @Override
        protected void executeTxCommit(Object object) {
            EventData eventData = (EventData)object;
            doASyncEvent(eventData.getEvent(),eventData.getData());
        }
    }

    /**
     * 如果在事务中，则放入事务缓存
     *
     * @param event 事件类型
     * @param data 事件数据
     * @return 是否放入事务缓存成功
     */
    private boolean checkAndAddTx(int event,Object data){
        EventData eventData = new EventData(event);
        eventData.setData(data);
        return eventTxLifeDepend.checkAndPut(eventData);
    }


    /**
     * 触发同步事件监听器
     *
     * @param event 事件类型
     * @param data 事件数据
     */
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

    /**
     * 触发异步事件监听器
     *
     * @param event 事件类型
     * @param data 事件数据
     */
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
     * 普通的抛出事件，事件同步还是异步执行，将根据监听器的设置决定
     *
     * @param event 事件id
     * @param data 事件参数
     */
    public void fireEvent(int event,Object data){
        // 同步的
        doSyncEvent(event, data);
        // 异步的
        doASyncEvent(event, data);
    }

    /**
     * 同步的抛出事件，事件按照同步方式执行，忽略监听器的设定
     *
     * @param event 事件id
     * @param data 事件参数
     */
    public void fireEventSyn(int event,Object data){
        Set<EventListenerHandler> handlerSet1 = handlerMap.get((short) event);
        Set<EventListenerHandler> handlerSet2 = synHandlerMap.get((short)event);

        if(handlerSet1 == null && handlerSet2 == null){
            return;
        }
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
