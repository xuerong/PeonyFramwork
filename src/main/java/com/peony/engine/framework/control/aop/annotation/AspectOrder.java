package com.peony.engine.framework.control.aop.annotation;

import com.peony.engine.framework.control.aop.AspectProxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 切面顺序。
 * <p>
 * 通过在切面类{@link Aspect}上添加<tt>AspectOrder<tt/>来决定该切面被执行的顺序。
 * value越小，越先执行，即包裹在多层切面的最外层。
 *
 * @author zhengyuzhen
 * @see AspectProxy
 * @see Aspect
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AspectOrder {
    /**
     * 切面被执行的顺序，越小越先执行.
     * @return
     */
    int value() default 10;
}
