package com.peony.engine.framework.security;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 每5分钟执行一次，将要监控的信息返回出来，
 * 有点鸡肋，为的是将监控信息统一打印
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {
//    int cycle() default 300000;
    String name();
}
