package com.peony.engine.framework.control.netEvent.remote;

import com.peony.engine.framework.control.aop.AspectProxy;
import com.peony.engine.framework.control.aop.annotation.Aspect;
import com.peony.engine.framework.tool.helper.BeanHelper;

import java.lang.reflect.Method;

/**
 * Created by apple on 16-10-4.
 */
@Aspect(
        annotation = {BroadcastRPC.class}
)
public class BroadcastRPCProxy extends AspectProxy {
    private BroadcastRPCService broadcastRPCService ;
    @Override
    public void before(Object object, Class<?> cls, Method method, Object[] params) {
        if(broadcastRPCService == null){
            broadcastRPCService = BeanHelper.getServiceBean(BroadcastRPCService.class);
        }
    }

    @Override
    public void after(Object object, Class<?> cls, Method method, Object[] params, Object result) {
        broadcastRPCService.afterMethod(object,cls,method,params,result);
    }

    @Override
    public void exceptionCatch(Throwable e) {

    }
}
