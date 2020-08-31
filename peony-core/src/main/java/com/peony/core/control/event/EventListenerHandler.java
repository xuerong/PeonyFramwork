package com.peony.core.control.event;

import com.peony.core.control.annotation.EventListener;

/**
 * 事件执行接口。
 * <p>
 * 所有存在{@link EventListener}的方法的类，都会生成对应的事件代理类，在代理类中实现
 * <tt>EventListenerHandler</tt>接口，在接口实现中，将事件通过{@code switch}导航
 * 到对应的方法。
 *
 * @author zhengyuzhen
 * @see EventListener
 * @see EventService
 * @since 1.0
 */
public interface EventListenerHandler {
    /**
     * 异步处理事件
     *
     * @param event
     * @param data
     * @throws Exception
     */
    void handle(int event,Object data) throws Exception;
    /**
     * 同步处理事件
     *
     * @param event
     * @param data
     * @throws Exception
     */
    void handleSyn(int event,Object data) throws Exception;
}
