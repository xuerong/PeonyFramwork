package com.peony.core.control.netEvent.remote;

import com.peony.core.control.ServiceHelper;
import com.peony.core.control.annotation.NetEventListener;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.netEvent.NetEventData;
import com.peony.core.control.netEvent.NetEventService;
import com.peony.core.control.rpc.RemoteExceptionHandler;
import com.peony.core.security.Monitor;
import com.peony.core.security.MonitorNumType;
import com.peony.core.security.MonitorService;
import com.peony.common.exception.MMException;
import com.peony.common.exception.ToClientException;
import com.peony.core.server.SysConstantDefine;
import com.peony.core.control.BeanHelper;
import com.peony.common.tool.thread.ThreadPoolHelper;
import com.peony.common.tool.util.Util;
import com.peony.common.tool.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

/**
 * Created by apple on 16-9-15.
 * TODO 把所有的方法补全
 */
@Service(init = "init")
public class RemoteCallService {
    private static final Logger logger = LoggerFactory.getLogger(RemoteCallService.class);
    private NetEventService netEventService;
    private MonitorService monitorService;
    private ExecutorService executorService = ThreadPoolHelper.newThreadPoolExecutor("PpeonyRemoteCall",32,256,65536);
    ConcurrentLinkedDeque<Integer> timeQueue = new ConcurrentLinkedDeque<>();
    private Map<String,Method> remoteMethodIndex;


    public void init(){
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
        remoteMethodIndex = ServiceHelper.getRemoteMethodIndex();
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
//            Method method = serviceBean.getClass().getMethod(remoteCallData.getMethodName(),
//                        ReflectionUtil.getParamsTypes(remoteCallData.getParamsObjectArray()));
            Method method = this.remoteMethodIndex.get(remoteCallData.getMethodSignature());

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
     * 此方法由 rpc 底层调用, (!!!勿动!!!)
     *
     * @param serverId
     * @param serviceClass
     * @param methodName
     * @param params
     * @return
     */
    public Object remoteCallSyn(int serverId, Class serviceClass, String methodName,String methodSignature, Object[] params,RemoteExceptionHandler handler) {
//    public Object remoteCallSyn(int serverId, Class serviceClass, String methodName, Object[] params,RemoteExceptionHandler handler) {
        long begin = System.currentTimeMillis();
        try {
            NetEventData netEventData = new NetEventData(SysConstantDefine.remoteCall);
            RemoteCallData remoteCallData = new RemoteCallData();
            remoteCallData.setServiceName(serviceClass.getName());
            remoteCallData.setMethodName(methodName);
            remoteCallData.setParams(params);
            remoteCallData.setMethodSignature(methodSignature);
            netEventData.setParam(remoteCallData);

            NetEventData retData = netEventService.fireServerNetEventSyn(serverId, netEventData);

            return handlerRetParam(retData);
        }catch (RuntimeException e){
            if(handler == null){
                throw e;
            }
            logger.error("remotecall error",e);
//            return handler.handle(serverId,serviceClass,methodName,params,e);
            return handler.handle(serverId,serviceClass,methodName,methodSignature,params,e);
        }finally {
            monitorService.addMonitorNum(MonitorNumType.RemoteCallNum,1);
            int useTime = (int)(System.currentTimeMillis() - begin);
            if(useTime > 3000){
                logger.warn("remoteCallSyn use time = {},serverId={},serviceClass={},methodName={},params={}",useTime,serverId,serviceClass.getName()
                        ,methodName,params);
            }
            timeQueue.add(useTime);
        }
    }

    @Monitor(name = "远程调用消耗时间")
    public String monitorRequest(){
        String date = DateUtils.formatNow("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        int size = timeQueue.size();
        if(size > 0) {
            int all = 0;
            int min=timeQueue.poll();
            int max = min;
            Map<Integer,Integer> useTime = new TreeMap<>();
            for (Integer value :timeQueue) {
                all += value;
                if(value >max){
                    max = value;
                }else if(value < min){
                    min = value;
                }
                Integer num = useTime.get(value);
                if(num==null){
                    num=0;
                }
                num++;
                int de = 1;
                while (value/de>10) {
                    de*=10;
                }
                useTime.put(value/de*de, num);
            }

            int average = all / size;

            sb.append(date).append("   ").append("【数量:").append(size).append(",平均用时:").
                    append(average).append(",最大用时：").append(max).append(",最小用时:").append(min).append("】;");
            for (Map.Entry<Integer,Integer> useTimeEntry : useTime.entrySet()){
                int value = useTimeEntry.getKey();
                int de = 1;
                while (value/de>=10) {
                    de*=10;
                }
                sb.append("[").append(value).append("-").append(value+de).append(":").append(useTimeEntry.getValue()).append("]");
            }
            sb.append("\n");
        }
        if(size>2000000) { // 1千万则重置，否则太大了
            ConcurrentLinkedDeque<Integer> newTimeQueue = new ConcurrentLinkedDeque<>();
            timeQueue = newTimeQueue;
        }
        return sb.toString();
    }

//    public void remoteCallAsync(int serverId, Class serviceClass, String methodName, Object[] params) {
    public void remoteCallAsync(int serverId, Class serviceClass, String methodName,String methodSignature,  Object[] params) {
        executorService.execute(()->{
            try{
                // TODO 这个地方要做一个真正的异步的能力，之后，就把线程池线程数量降下来
                remoteCallSyn(serverId, serviceClass, methodName, methodSignature,params, null);
            }catch (Throwable e){
                logger.error("remoteCallAsync error!",e);
            }
        });
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
    public <T> Map<Integer,T> broadcastRemoteCallSyn(Class cls, String methodName, String methodSignature,Object... params){
        NetEventData netEventData = new NetEventData(SysConstantDefine.remoteCall);
        RemoteCallData remoteCallData = new RemoteCallData();
        remoteCallData.setServiceName(cls.getName());
        remoteCallData.setMethodName(methodName);
        remoteCallData.setParams(params);
        remoteCallData.setMethodSignature(methodSignature);
        netEventData.setParam(remoteCallData);

        Map<Integer,NetEventData> retData = netEventService.broadcastNetEventSyn(netEventData,false);
        Map<Integer,T> result = new HashMap(retData.size());
        for(Map.Entry<Integer,NetEventData> re : retData.entrySet()){
            result.put(re.getKey(),(T)re.getValue().getParam());
        }
        return result;
    }
}
