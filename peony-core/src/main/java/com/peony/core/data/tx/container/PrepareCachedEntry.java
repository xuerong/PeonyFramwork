package com.peony.core.data.tx.container;

import com.peony.core.data.OperType;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-08-28 16:02
 */
public class PrepareCachedEntry<K,V> {
    private OperType operType;
    private K key;
    private V data;

    public OperType getOperType() {
        return operType;
    }

    public void setOperType(OperType operType) {
        this.operType = operType;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getData() {
        return data;
    }

    public void setData(V data) {
        this.data = data;
    }
}
