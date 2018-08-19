package com.peony.engine.framework.control.netEvent.remote;

import com.peony.engine.framework.control.annotation.NetEventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.netEvent.NetEventData;
import com.peony.engine.framework.control.netEvent.NetEventService;
import com.peony.engine.framework.control.netEvent.ServerInfo;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.util.ReflectionUtil;
import com.peony.engine.framework.tool.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by apple on 16-9-15.
 * TODO 把所有的方法补全
 */
@Service(init = "init")
public class RemoteCallService {
    private static final Logger logger = LoggerFactory.getLogger(RemoteCallService.class);
    private NetEventService netEventService;

    public void init(){
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
    }

    /**
     * 异步远程调用
     * TODO 删除之
     */
    public void remoteCallMainServer(Class cls,String methodName,Object... params){
        NetEventData netEventData = new NetEventData(SysConstantDefine.remoteCall);
//        Method method = null;
//        try {
//            method = cls.getMethod(methodName, ReflectionUtil.getParamsTypes(params));
//        }catch (NoSuchMethodException e){
//            throw new MMException(e);
//        }

        RemoteCallData remoteCallData = new RemoteCallData();
        remoteCallData.setServiceName(cls.getName());
        remoteCallData.setMethodName(methodName);
        remoteCallData.setParams(params);
        netEventData.setParam(remoteCallData);
        netEventService.fireServerNetEvent(1,netEventData);
    }

    @NetEventListener(netEvent = SysConstantDefine.remoteCall)
    public NetEventData receiveRemoteCall(NetEventData netEventData){
        RemoteCallData remoteCallData = (RemoteCallData)netEventData.getParam();
        try {
            Object serviceBean = BeanHelper.getServiceBean(remoteCallData.getServiceName());
            if(serviceBean == null){
                logger.error("remote call error!serviceBean is not exit!,serviceName = {}",remoteCallData.getServiceName());
                RemoteCallError remoteCallError = new RemoteCallError();
                remoteCallError.setMsg(Util.formatMsg("remote call error!serviceBean is not exit!,serviceName = {}",remoteCallData.getServiceName()));
                netEventData.setParam(remoteCallError);
                netEventData.setErrorCode(1);
                return netEventData;
            }
            Method method = serviceBean.getClass().getMethod(remoteCallData.getMethodName(),
                        ReflectionUtil.getParamsTypes(remoteCallData.getParams()));
            if(method == null){
                logger.error("remote call error!serviceBean {} has not method {},params={}",remoteCallData.getServiceName(),
                        remoteCallData.getMethodName(),remoteCallData.getParams());
                RemoteCallError remoteCallError = new RemoteCallError();
                remoteCallError.setMsg(Util.formatMsg("remote call error!serviceBean {} has not method {},params={}",remoteCallData.getServiceName(),
                        remoteCallData.getMethodName(),remoteCallData.getParams()));
                netEventData.setParam(remoteCallError);
                netEventData.setErrorCode(2);
                return netEventData;
            }
            Object ret = method.invoke(serviceBean, remoteCallData.getParams());
            if(!(ret instanceof Void)) {
                netEventData.setParam(ret);
            }
            return netEventData;
        }catch (Throwable e){
            if(e instanceof InvocationTargetException){
                e = e.getCause();
            }
            if(e instanceof ToClientException){
                ToClientException toClientException = (ToClientException)e;
                netEventData.setParam(toClientException.toParams());
                logger.info("receiveRemoteCall ToClientException exception,code={},msg={}:",toClientException.getErrCode(),toClientException.getMessage());
                netEventData.setErrorCode(3);
                return netEventData;
            }
            netEventData.setParam(e);

            logger.error("do receiveRemoteCall error!",e);
            netEventData.setErrorCode(4);
            return netEventData;
        }
    }

    /**
     * 同步远程调用
     * TODO 删除之
     */
    public Object remoteCallMainServerSyn(Class cls,String methodName,Object... params){
        NetEventData netEventData = new NetEventData(SysConstantDefine.remoteCall);
        RemoteCallData remoteCallData = new RemoteCallData();
        remoteCallData.setServiceName(cls.getName());
        remoteCallData.setMethodName(methodName);
        remoteCallData.setParams(params);
        netEventData.setParam(remoteCallData);

        NetEventData retData = netEventService.fireServerNetEventSyn(1,netEventData);
        return handlerRetParam(retData);
    }

    /**
     * 此方法由 rpc 底层调用, (!!!勿动!!!)
     *
     * @param serverId
     * @param serviceClass
     * @param methodName
     * @param params
     * @return
     */
    public Object remoteCallSyn(int serverId, Class serviceClass, String methodName, Object[] params) {

        NetEventData netEventData = new NetEventData(SysConstantDefine.remoteCall);
        RemoteCallData remoteCallData = new RemoteCallData();
        remoteCallData.setServiceName(serviceClass.getName());
        remoteCallData.setMethodName(methodName);
        remoteCallData.setParams(params);
        netEventData.setParam(remoteCallData);

        NetEventData retData = netEventService.fireServerNetEventSyn(serverId, netEventData);

        return handlerRetParam(retData);
    }

    private Object handlerRetParam(NetEventData retData){
        if(retData.getParam() instanceof RemoteCallError){
            RemoteCallError remoteCallError = (RemoteCallError)retData.getParam();
            // 同步远程调用失败
            throw new MMException("remoteCallError!{}",remoteCallError);
        }else if(retData.getErrorCode() == 3){
            ToClientException toClientException = ToClientException.parseFromParams((Object[]) retData.getParam());
            throw toClientException;
        }else if(retData.getParam() instanceof MMException){
            throw (MMException)retData.getParam();
        }else if(retData.getParam() instanceof Throwable){
            throw new MMException((Throwable)retData.getParam());
        }

        return retData.getParam();
    }

    // TODO 广播还要同步，如果处理返回值
    public <T> Map<Integer,T> broadcastRemoteCallSyn(Class cls, String methodName, Object... params){
        NetEventData netEventData = new NetEventData(SysConstantDefine.remoteCall);
        RemoteCallData remoteCallData = new RemoteCallData();
        remoteCallData.setServiceName(cls.getName());
        remoteCallData.setMethodName(methodName);
        remoteCallData.setParams(params);
        netEventData.setParam(remoteCallData);

        Map<Integer,NetEventData> retData = netEventService.broadcastNetEventSyn(netEventData,false);
        Map<Integer,T> result = new HashMap(retData.size());
        for(Map.Entry<Integer,NetEventData> re : retData.entrySet()){
            result.put(re.getKey(),(T)re.getValue().getParam());
        }
        return result;
    }
}
