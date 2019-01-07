package com.peony.engine.framework.control.aop;

import java.lang.reflect.Method;

/**
 * 代理类接口。
 * 当一个类需要被代理时，实则是对其方法进行继承重写，来添加额外的代码，或者替换。
 */
public interface Proxy {
    /**
     * 被代理的方法执行之前。
     *
     * @param object 对应的对象
     * @param cls 对象类型
     * @param method 被代理的方法
     * @param params 方法参数
     */
    void before(Object object,Class<?> cls, Method method, Object[] params);
    /**
     * 被代理的方法执行之后。
     *
     * @param object 对应的对象
     * @param cls 对象类型
     * @param method 被代理的方法
     * @param params 方法参数
     */
    void after(Object object,Class<?> cls, Method method, Object[] params, Object result);

    /**
     * 被代理的方法抛出异常
     *
     * @param e 抛出的异常
     */
    void exceptionCatch(Throwable e);

    /**
     * 该方法是否满足执行的条件。
     *
     * @param method 被代理的方法
     * @return
     */
    boolean executeMethod(Method method);

    /**
     * 设置被代理的目标类。
     *
     * @param targetClass 目标类
     */
    void setTargetClass(Class<?> targetClass);
}
