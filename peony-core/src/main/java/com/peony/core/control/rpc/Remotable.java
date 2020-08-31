package com.peony.core.control.rpc;

import com.peony.core.control.annotation.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @see Service
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Remotable {
    /**
     * 路由策略
     *
     * @return
     */
    RouteType route();

    /**
     * 用于计算路由的 参数 index 从1开始
     *
     * @return
     */
    int routeArgIndex() default 1;

    /**
     * 用于远程调用的异常处理
     */
    RemoteExceptionHandler remoteExceptionHandler() default RemoteExceptionHandler.Default;

}

