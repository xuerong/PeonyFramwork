package com.peony.engine.framework.control.job;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.tool.util.SerializeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by apple on 16-10-2.
 */
@DBEntity(tableName = "job",pks = {"id"})
public class Job implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    private long id;
    private int serverId; // 哪个服务器上的job
    //
    private Timestamp startDate; // 执行时间,之所以不用delay，是因为如重启服务器的时候要加载job，

    private String method;
    private String serviceClass;



    private byte[] params; // 参数要能够序列化

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

    public byte[] getParams() {
        return params;
    }

    public void setParams(byte[] params) {
        this.params = params;
    }

    public Object[] getParamsObjectArray() {
        try {
            return (Object[])SerializeUtil.deserialize(getParams(), Object[].class);
        }catch (Exception e){
            logger.error("getParamsObjectArray error! id={}",id,e);
            return null;
        }
    }

    public void setParamsObjectArray(Object[] params) {
        try {
            this.params = SerializeUtil.serialize(params);
        }catch (Exception e){
            logger.error("setParamsObjectArray error!id={}",id,e);
        }
    }
}
