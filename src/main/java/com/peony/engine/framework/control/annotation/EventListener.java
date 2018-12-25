package com.peony.engine.framework.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2015/11/16.
 * 需要监听一个事件的函数添加此注释，并将event参数定义为需要监听的事件
 * 要求，监听事件接收一个参数EventData
 *
 * 每个event可以有多个处理方法，分布在多个Service中
 *
 * TODO Event要不要广播到其它服务器，比如一个玩家的升级？
 *
 *
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener{
    /**
     * event,
     * **/
    int event();
    /**
     * TODO 是否同步，也就是说同步也异步决定于监听方
     */
    boolean sync() default false;
}
