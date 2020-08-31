package com.peony.demo.proxyTest;

import com.peony.core.control.aop.AspectProxy;
import com.peony.core.control.aop.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2015/11/17.
 */

@Aspect(
        mark = {"aa"}
//        annotation = {Request.class}
        //pkg = {"com.peony.engine.sysBean.controller"}
)
public class MyProxy extends AspectProxy {
    private static final Logger log = LoggerFactory.getLogger(MyProxy.class);
    @Override
    public void before(Object object,Class<?> cls, Method method, Object[] params) {
        log.info("before");
    }

    @Override
    public void after(Object object,Class<?> cls, Method method, Object[] params, Object result) {
        log.info("after");
    }

    @Override
    public void exceptionCatch(Throwable e) {

    }
}
