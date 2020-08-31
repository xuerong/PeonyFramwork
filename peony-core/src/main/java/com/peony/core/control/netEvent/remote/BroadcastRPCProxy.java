package com.peony.core.control.netEvent.remote;

import com.peony.core.control.aop.AspectProxy;
import com.peony.core.control.aop.annotation.Aspect;
import com.peony.core.control.BeanHelper;

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
