package com.peony.engine.framework.control.job;

import java.util.Date;

/**
 * Created by apple on 16-9-4.
 * job“为多久之后执行一个事情”
 * 对于周期性的执行事情，在updatable中处理
 *
 */
public class Job{
    private String id;
    //
    private Date startDate; // 执行时间,之所以不用delay，是因为如重启服务器的时候要加载job，

    private boolean db; // 是否持久化，跟随系统启动而启动的一般不需要db，

    private String method;
    private Class serviceClass;
    private Object[] para;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public boolean isDb() {
        return db;
    }

    public void setDb(boolean db) {
        this.db = db;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Class getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Object[] getPara() {
        return para;
    }

    public void setPara(Object... para) {
        this.para = para;
    }

}
