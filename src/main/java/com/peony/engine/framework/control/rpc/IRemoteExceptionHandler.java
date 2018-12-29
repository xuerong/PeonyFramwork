package com.peony.engine.framework.control.rpc;

public interface IRemoteExceptionHandler {
    public Object handle(int serverId, Class serviceClass, String methodName,String methodSignature, Object[] params, RuntimeException e);
}
