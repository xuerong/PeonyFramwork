package com.peony.engine.framework.control.gm;

import java.lang.reflect.Method;

/**
 * Created by a on 2016/9/29.
 */
public class GmSegment {
    private String id;
    private String describe;
    private Method method;
    private Class[] paramsType;
    private String[] paramsName;
    private Object service;
    private Class returnType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class[] getParamsType() {
        return paramsType;
    }

    public void setParamsType(Class[] paramsType) {
        this.paramsType = paramsType;
    }

    public String[] getParamsName() {
        return paramsName;
    }

    public void setParamsName(String[] paramsName) {
        this.paramsName = paramsName;
    }

    public Object getService() {
        return service;
    }

    public void setService(Object service) {
        this.service = service;
    }

    public Class getReturnType() {
        return returnType;
    }

    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }
}
