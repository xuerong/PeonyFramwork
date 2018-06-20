package com.peony.engine.framework.control.gm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by a on 2016/9/28.
 * Gm 方法的返回值，要么是void，要么是map<String,String>，要么是string
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Gm {
    /**
     * 用string类型的id，这样好设计，不用再用共用的id文档
     * 主要是gm不需要更高的效率，不需要添加switch控制转发，通过map存储，用反射调用就可以
     * @return
     */
    String id();
    String describe() default "";

    /**
     * 参数的名字,会显示在gm界面的参数之前,不配置则显示param
     * @return
     */
    String[] paramsName() default {};
}
