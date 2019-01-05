package com.peony.engine.framework.control.aop.annotation;

import com.peony.engine.framework.control.annotation.Service;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/11/17.
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @see Service
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AspectMark {
    String[] mark();
}
