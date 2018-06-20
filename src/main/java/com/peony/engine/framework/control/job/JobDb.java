package com.peony.engine.framework.control.job;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by apple on 16-10-2.
 */
@DBEntity(tableName = "job",pks = {"id"})
public class JobDb implements Serializable {
    private String id;
    //
    private Timestamp startDate; // 执行时间,之所以不用delay，是因为如重启服务器的时候要加载job，

    private int db; // 是否持久化，跟随系统启动而启动的一般不需要db，

    private String method;
    private String serviceClass;
    private Object[] params; // 参数要能够序列化

    public JobDb(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public int getDb() {
        return db;
    }

    public void setDb(int db) {
        this.db = db;
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
