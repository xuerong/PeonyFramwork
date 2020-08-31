package com.peony.core.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时更新器。
 * <p>
 * 添加此注解的方法将在每隔一段时间被调用一次。提供了两种调用策略，一种是每隔一个固定
 * 的时间执行一次，一种是通过一个<tt>CronExpression</tt>表达式来定义执行的时间。
 * <p>
 * 对应的方法满足：1、一个<code>int</code>参数，代表与上一次执行的时间间隔。2、返回值
 * 为void。比如：
 * <pre>
 *     public void updatable(int cycle){...}
 * </pre>
 * <p>
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @author zhengyuzhen
 * @see com.peony.core.control.update.UpdateService
 * @see Service
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Updatable {

    /**
     * 启动是否执行一次，在<tt>cycle</tt>生效时生效
     */
    boolean doOnStart() default false;

    /**
     * 更新周期，毫秒计。该参数与<tt>cronExpression</tt>至少要设置一个
     */
    int cycle() default -1;

    /**
     * cronExpression表达式。
     * 该参数与<tt>cycle</tt>至少要设置一个。如果设置了该参数，<tt>cycle</tt>不再生效，
     * <tt>doOnStart</tt>也不再生效
     */
    String cronExpression() default "";

}
