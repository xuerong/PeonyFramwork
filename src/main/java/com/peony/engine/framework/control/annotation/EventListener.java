package com.peony.engine.framework.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件监听器.
 * <p>
 * 事件是peony中的一种信息交互方式。当一个事件被抛出，监听这个事件的监听器都会被执行。
 * <p>
 * 通过给一个方法添加<code>EventListener</code>的方式，实现事件监听。实现事件监听
 * 的方法必须满足如下要求：1、参数为一个，且类型为发送事件带的参数类型；2、返回值为
 * void。比如：
 * <pre>
 *     public void listener(Object eventData){...}
 * </pre>
 * <p>
 * <strong>每个event可以有多个处理方法，分布在多个Service中</strong><p>
 * <strong>每个方法只能添加一个<code>EventListener</code>注解</strong><p>
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @author zhengyuzhen
 * @see com.peony.engine.framework.control.event.EventService
 * @see Service
 * @since 1.0
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener{
    /**
     * 事件编号
     * @return
     */
    int event();

    /**
     * 是否同步
     * 为<code>true</code>时，由抛出事件的线程同步执行
     * 为<code>false</code>时，执行方式决定于抛出事件的同步/异步方式
     * @return
     */
    boolean sync() default false;
}
