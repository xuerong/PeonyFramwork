package com.peony.engine.framework.data.tx;

import com.peony.engine.framework.data.OperType;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-08-28 09:38
 */
public class PrepareCachedData {
    private OperType operType;
    // TODO 这个key，如果更新可以的话，是更新之后的数据的key，而不是，更新之前的key，在校验时会有问题，更新缓存的地方也要看下！要改成更新之前的key！！！！
    private String key;
    private Object data;

    public OperType getOperType() {
        return operType;
    }

    public void setOperType(OperType operType) {
        this.operType = operType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
