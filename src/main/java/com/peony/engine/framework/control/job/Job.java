package com.peony.engine.framework.control.job;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by apple on 16-10-2.
 */
@DBEntity(tableName = "job",pks = {"id"})
public class Job implements Serializable {
    private long id;
    private int serverId; // 哪个服务器上的job
    //
    private Timestamp startDate; // 执行时间,之所以不用delay，是因为如重启服务器的时候要加载job，

    private String method;
    private String serviceClass;
    private Object[] params; // 参数要能够序列化

    public Job(){}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }
}
