package com.peony.engine.framework.control.statistics;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by a on 2016/9/28.
 * Statistics 方法的返回值，StatisticsData
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Statistics {
    /**
     * 用string类型的id，这样好设计，不用再用共用的id文档
     * 主要是gm不需要更高的效率，不需要添加switch控制转发，通过map存储，用反射调用就可以
     * @return
     */
    String id();
    String name();
    String describe() default "";
}
