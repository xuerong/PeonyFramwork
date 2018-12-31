package com.peony.engine.framework.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2015/11/16.
 * 更新器，用于做定时更新
 * 添加此注解的方法将在每隔一段时间被调用一次
 * Updatable分为同步和异步
 *
 * 对应的方法存在一个参数。实际的更新时间间隔
 *
 * 后面可以改成cronExpression
 * job是job，只执行一次,如果想通过job实现多次执行的话，就自己实现，显然那一般不需要
 *
 *
 * @Updatable(cycle = 60000)
    public void monitorCacheNum(int cycle){

    }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Updatable {
    /**
     * interval,更新周期，毫秒计
     * 异步更新时有效
     * 这个可以用定义系统参数的方式，就可以不写死了
     * **/
    int cycle() default -1;
    /**
     * 启动就执行一次？
     */
    boolean doOnStart() default false;

    /**
     * cronExpression 表达式，这个标志着cycle和isAsynchronous都不再使用
     */
    String cronExpression() default "";

}
