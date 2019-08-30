package com.peony.engine.framework.data.tx.container;

import com.peony.engine.framework.security.exception.MMException;

import java.util.Map;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-08-27 17:38
 */
public interface TxMap<K,V> extends Map<K,V> ,TxContainer{


    V readNotUpdate(K key);
    V readForUpdate(K key);

    default V get(Object key){
        throw new MMException("not support!please use readNotUpdate (read only , not for update) or readForUpdate (read for update)");
    }


//    int size();
//    boolean isEmpty();
//    boolean containsKey(K key);
//    boolean containsValue(V value);
//    V readNotUpdate(K key);
//    V readForUpdate(K key);
//    V put(K key,V value);
//    V putIfAbsent(K key,V value);
//    V remove(K key);
//
//    void putAll(TxMap m);
//    void putAll(Map m);
//    Set<K> keySet();
//    void clear();
//    Collection<V> values();
//    Set<Map.Entry<K,V>> entrySet();

}
