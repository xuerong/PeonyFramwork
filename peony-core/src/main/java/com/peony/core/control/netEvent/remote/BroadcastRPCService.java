package com.peony.core.control.netEvent.remote;

import com.peony.core.control.ServiceHelper;
import com.peony.core.control.annotation.NetEventListener;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.netEvent.NetEventData;
import com.peony.core.control.netEvent.NetEventService;
import com.peony.common.exception.MMException;
import com.peony.core.server.SysConstantDefine;
import com.peony.core.control.BeanHelper;
import com.peony.common.tool.util.ClassUtil;
import com.peony.common.tool.util.ReflectionUtil;

import java.lang.reflect.Method;

/**
 * Created by apple on 16-10-4.
 */
@Service(init = "init")
public class BroadcastRPCService {
    private NetEventService netEventService;

    private ThreadLocal<Boolean> isRpc = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };

    public void init() {

    }

    public void afterMethod(Object object, Class<?> cls, Method method, Object[] params, Object result){
        if(isRpc.get()){
            BroadcastRPC broadcastRPC = method.getAnnotation(BroadcastRPC.class);
            NetEventData netEventData = new NetEventData(SysConstantDefine.broadcastRPC);
            RemoteCallData remoteCallData = new RemoteCallData();
            Class<?> originClass = ServiceHelper.getOriginServiceClass(cls);
            remoteCallData.setServiceName(originClass.getName());
            remoteCallData.setMethodName(method.getName());
            remoteCallData.setParams(params);
            netEventData.setParam(remoteCallData);
            remoteCallData.setMethodSignature(ClassUtil.getMethodSignature(method));
            if(broadcastRPC.async()){
                netEventService.broadcastNetEvent(netEventData,false);
            }else {
                netEventService.broadcastNetEventSyn(netEventData,false);
            }
        }
    }

    @NetEventListener(netEvent = SysConstantDefine.broadcastRPC)
    public NetEventData receiveBroadcastRPC(NetEventData netEventData){
        isRpc.set(false);
        RemoteCallData remoteCallData = (RemoteCallData)netEventData.getParam();
        try {
            Object serviceBean =  BeanHelper.getServiceBean(remoteCallData.getServiceName());
            if(serviceBean == null){

            }
            Method method = serviceBean.getClass().getMethod(remoteCallData.getMethodName(),
                    ReflectionUtil.getParamsTypes(remoteCallData.getParams()));
            Object ret = method.invoke(serviceBean, remoteCallData.getParams());
            if(!(ret instanceof Void)) {
                netEventData.setParam(ret);
            }
            return netEventData;
        }catch (Throwable e){
            throw new MMException(e);
        }finally {
            isRpc.set(true);
        }
    }
}
