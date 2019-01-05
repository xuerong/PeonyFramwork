package com.peony.engine.framework.control.netEvent.remote;

import com.peony.engine.framework.control.annotation.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by apple on 16-10-4.
 * 如果一个方法添加了该注解,该方法被外部调用的时候会广播其它服务器调用该方法
 * 用于替换手写的远程调用执行同样的方法
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @see Service
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BroadcastRPC {
    boolean async() default false;
}
