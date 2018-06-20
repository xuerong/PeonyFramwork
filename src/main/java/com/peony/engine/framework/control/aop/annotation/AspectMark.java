package com.peony.engine.framework.control.aop.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/11/17.
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AspectMark {
    String[] mark();
}
