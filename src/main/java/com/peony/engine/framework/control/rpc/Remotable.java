package com.peony.engine.framework.control.rpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

}

