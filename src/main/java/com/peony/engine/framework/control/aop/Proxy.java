package com.peony.engine.framework.control.aop;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2015/11/17.
 * 如非特殊需要，自定义切面代理类请继承AspectProxy
 */
public interface Proxy {
    public void before(Object object,Class<?> cls, Method method, Object[] params);
    public void after(Object object,Class<?> cls, Method method, Object[] params, Object result);
    public void exceptionCatch(Throwable e);
    public boolean executeMethod(Method method);
    public void setTargetClass(Class<?> targetClass);
}
