package com.peony.core.control.aop.annotation;

import com.peony.core.control.aop.AopHelper;
import com.peony.core.control.aop.AspectProxy;

import java.lang.annotation.*;

/**
 * Aop切面类注解.
 * <p>
 * AOP功能用于在一组特定方法（切点）前后添加处理代码，所添加的代码称为切面，对应一个切面类。
 * 切面类需要继承自抽象类{@link AspectProxy}，同时添加<tt>Aspect</tt>注解。AspectProxy
 * 提供了需要继承的方法，<tt>Aspect</tt>提供了切面适配的方法规则，即哪些方法添加该AOP功能。
 *
 * <p>
 * 注解方式分为以下几种,满足其中一种即可：
 * <ol>
 *     <li>通过对方类或方法添加{@link AspectMark}注解，实现通过字符串匹配的方式实现。即如果
 *     AspectMark的mark数组中包含该Aspect中mark参数，即添加了该AOP功能</li>
 *     <li>对于添加了某种注解的类或方法做切面，可以直接添加在annotation参数中实现</li>
 *     <li>对于某个包之下的所有类的所有方法做切面，可以直接添加包在pkg参数中实现</li>
 * </ol>
 *
 * @author zhengyuzhen
 * @see AspectProxy
 * @see AspectMark
 * @see AspectOrder
 * @see AopHelper
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aspect {

    /**
     * 如果AspectMark的mark数组中包含该Aspect中mark参数，即添加了该AOP功能
     * @return
     */
    String[] mark() default {};

    /**
     * 对于添加了某种注解的类或方法做切面，可以直接添加在annotation参数中实现
     * @return
     */
    Class<? extends Annotation>[] annotation() default  {};

    /**
     * 对于某个包之下的所有类的所有方法做切面，可以直接添加包在pkg参数中实现
     */
    String[] pkg() default {};
}
