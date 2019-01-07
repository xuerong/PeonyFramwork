package com.peony.engine.framework.control.aop.annotation;

import com.peony.engine.framework.control.annotation.Service;

import java.lang.annotation.*;

/**
 * AOP切点标记器。
 * <p>
 * 可以对{@link Service}类或者里面的方法添加该注解。参数中的mark将与切面类{@link Aspect}
 * 中的mark进行匹配，来决定该类中的方法或者改方法添加的切面。
 * <p>
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @author zhengyuzhen
 * @see Service
 * @see AspectMark
 * @since 1.0
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface AspectMark {
    /**
     * 标记数组，一次可以定义多个标记，来添加多层AOP切面。
     * @return
     */
    String[] mark();
}
