package com.peony.engine.framework.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 网络事件监听器.
 * <p>
 * 网络事件是用于在不同服务器之间实现事件监听的方法，由一个服务器抛出，其它一个或多个
 * 服务器监听。peony中远程调用是通过<code>NetEventListener</code>实现的。
 * <p>
 * 通过给一个方法添加<code>NetEventListener</code>，实现网络事件监听。实现网络事
 * 件监听的方法必须满足如下要求：1、接受一个<code>NetEventData</code>类型的参数；
 * 2、返回一个<code>NetEventData</code>类型的参数，或者不返回数据。比如：
 * <pre>
 *     public NetEventData netEventListener(NetEventData netEventData){...}
 * </pre>
 *
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @author zhengyuzhen
 * @see com.peony.engine.framework.control.netEvent.NetEventService
 * @see com.peony.engine.framework.control.rpc.Remotable
 * @see Service
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetEventListener {
    /**
     * 网络事件编号
     * @return
     */
    int netEvent();
}
