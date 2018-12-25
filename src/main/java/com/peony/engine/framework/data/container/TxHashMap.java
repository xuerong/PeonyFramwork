package com.peony.engine.framework.data.container;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持事务的hashMap
 *
 * 我们需要实现的东西是多样的，包括：
 *
 * 只读性      DBEntity
 * 非只读性    非DBEntity
 *
 * 1、缓存对象是DBEntity，分为两种：只读性（即不能从这里面取出数据后修改），非只读性
 * 2、缓存对象不是DBEntity，要考虑深层复制
 * 3、多层缓存
 *
 *
 * get时，获取拷贝
 * put时，(如果需要，先进行版本校验)，放入tx缓存
 *
 * 提交时才真正放入map
 */
public class TxHashMap<K,V> extends ConcurrentHashMap<K,V> implements TxContainer{

    /**
     * 对于大多数缓存对象，要做到
     *
     * @param syncDBEntity 是否是数据库对象的副本。
     */
    TxHashMap(boolean syncDBEntity){

    }


    @Override
    public V get(Object key) {
        return null;
    }

    @Override
    public V put(K key, V value) {
        return null;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Override
    public void txCommit() {

    }
}
