package com.peony.core.control.job;

import com.peony.core.data.persistence.orm.annotation.DBEntity;
import com.peony.common.tool.util.SerializeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Job对象。
 * <p>
 * Job指过一段时间后执行某个方法。Job对象有两个作用：1、保存一段时间后执行的一个Job，在执行
 * 时间点到了之后取出来执行，2、当Job启动时保存在数据库中，当服务器重启时会加载出来，确保Job
 * 不会因为服务器关闭被清除。
 *
 * @author zhengyuzhen
 * @see JobService
 * @since 1.0
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
