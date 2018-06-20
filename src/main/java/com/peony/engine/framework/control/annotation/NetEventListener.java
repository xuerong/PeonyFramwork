package com.peony.engine.framework.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2015/11/17.
 *
 * 需要监听一个网络事件的函数添加此注释，并将netEvent参数定义为需要监听的事件
 * 要求，监听事件接收一个参数NetEventData,返回NetEventData对象,或者不返回数据
 *
 *
 * 网络事件一般是指，由其它服务器传递过来的数据，用于一致性校验等
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetEventListener {
    /**
     * netEvent,
     * **/
    int netEvent();
}
