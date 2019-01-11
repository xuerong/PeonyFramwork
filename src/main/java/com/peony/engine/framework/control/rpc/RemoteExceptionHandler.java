package com.peony.engine.framework.control.rpc;

import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum RemoteExceptionHandler implements IRemoteExceptionHandler {

    Default(){
        @Override
        public Object handle(int serverId, Class serviceClass, String methodName,String methodSignature, Object[] params, RuntimeException e) {
            log.info("RemoteExceptionHandler default",e);
            throw  e;
        }
    },
    Null(){
        @Override
        public Object handle(int serverId, Class serviceClass, String methodName,String methodSignature, Object[] params, RuntimeException e) {
            log.info("RemoteExceptionHandler Null",e);
            return null;
        }
    },

    AddQueue(){ // 如果不是ToClientException，则加入队列
        @Override
        public Object handle(int serverId, Class serviceClass, String methodName,String methodSignature, Object[] params, RuntimeException e) {
            if(e instanceof MMException){
                MMException mmException = (MMException)e;
                if(mmException.getExceptionType() == MMException.ExceptionType.RemoteFail){
                    //加入缓存队列
                    RemoteExceptionQueueService remoteExceptionQueueService = BeanHelper.getServiceBean(RemoteExceptionQueueService.class);
                    remoteExceptionQueueService.addQueue(serverId,serviceClass,methodName,methodSignature,params,e);
                    return null;
                }
            }
            throw e;
        }
    },

    ;
    private static final Logger log = LoggerFactory.getLogger(RemoteExceptionHandler.class);
}
