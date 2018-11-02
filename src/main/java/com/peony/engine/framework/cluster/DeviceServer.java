package com.peony.engine.framework.cluster;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * main服用的，设备id对应服务器id的表
 */
@DBEntity(tableName = "main_deviceserver",pks = {"deviceId"})
public class DeviceServer implements Serializable{

    private String deviceId;
    private int serverId;
    private long regTime;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public long getRegTime() {
        return regTime;
    }

    public void setRegTime(long regTime) {
        this.regTime = regTime;
    }
}
