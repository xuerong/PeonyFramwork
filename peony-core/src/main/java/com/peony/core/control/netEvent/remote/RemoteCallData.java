package com.peony.core.control.netEvent.remote;

import java.io.Serializable;

/**
 * Created by apple on 16-9-15.
 */
public class RemoteCallData implements Serializable{
    private String serviceName;
    private String methodName;
    private Object[] params;
//    private String key; 如果需要可以在传给服务器之前调用buildMethodKey给它赋值,来提高singleServiceServer效率
    private String methodSignature;


    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }
}
