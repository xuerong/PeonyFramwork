package com.peony.engine.framework.control.aop.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2015/11/17.
 *
 * Aspect用来注解切面类AspectProxy，
 * 注解方式分为以下几种,满足其中一种即可
 * 1、对于用AspectMark标注的类或方法，其中有mark字符串存在于Aspect的mark参数中的
 * 2、对于添加了某种注解的类或方法
 * 3、对于某个包之下的所有类的所有方法
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {
    /**
     * marks
     * */
    String[] mark() default {};
    /**
     * 注解
     */
    Class<? extends Annotation>[] annotation() default  {};
    /**
     * 包名
     */
    String[] pkg() default {};
}
