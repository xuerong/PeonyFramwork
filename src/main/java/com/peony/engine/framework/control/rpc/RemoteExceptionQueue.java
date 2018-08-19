package com.peony.engine.framework.control.rpc;


import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "remoteexceptionqueue",pks = {"id","serverId"})
public class RemoteExceptionQueue implements Serializable{
    private String id;
    private int serverId;
    // int serverId, Class serviceClass, String methodName, Object[] params, RuntimeException
    private String serviceClass;
    private String methodName;
    private byte[] params;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public byte[] getParams() {
        return params;
    }

    public void setParams(byte[] params) {
        this.params = params;
    }

    @Override
    public String toString(){
        return new StringBuilder("id:").append(id).append(",serverId:").append(serverId).append(",serviceClass:")
                .append(serviceClass).append(",methodName:").append(methodName).toString();
    }
}
