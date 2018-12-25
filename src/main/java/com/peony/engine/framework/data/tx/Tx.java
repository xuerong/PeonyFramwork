package com.peony.engine.framework.data.tx;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2015/11/16.
 * 所有的服务方法注解继承自它，
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tx {
    /**
     * isolation="DEFAULT/READ_COMMITTED/READ_UNCOMMITTED/REPEATABLE_READ/SERIALIZABLE"
     * PROPAGATION=PROPAGATION_REQUIRED ,PROPAGATION_SUPPORTS,PROPAGATION_MANDATORY ,PROPAGATION_REQUIRES_NEW ,PROPAGATION_NOT_SUPPORTED,PROPAGATION_NEVER,PROPAGATION_NESTED
     * 两种方案：
     * 1、支持事务定死READ_COMMITTED和PROPAGATION_REQUIRED，服务编写者只提供是否支持事务的设置
     * 2、服务编写者写isolation和PROPAGATION
     * **/
    boolean tx() default true;
    boolean lock() default true;
    Class<?>[] lockClass() default {};
}