package com.peony.core.data.tx.container;

import com.peony.core.data.OperType;
import com.peony.core.data.persistence.orm.EntityHelper;
import com.peony.common.exception.MMException;
import com.peony.core.control.BeanHelper;
import org.apache.commons.collections.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * put时，先放入事务，事务提交提交之
 *
 * 事务需要完成的是：
 * 当读取时为了修改时，需要拷贝返回，并记录引用，提交时，引用替换，
 *
 *
 */
public class TxHashMap<K,V> implements TxMap<K,V>{

    ConcurrentHashMap<K,V> _map = new ConcurrentHashMap<>();

    ThreadLocal<Map<K, PrepareCachedEntry<K,V>>> cachedDataThreadLocal = new ThreadLocal<>();


    @Override
    public void txBegin() {
        throw new MMException("will not do it!");
    }

    @Override
    public void txCommitSuccess() {
        // 将数据保存到_map，清空线程缓存
        Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
        if(map != null){
            cachedDataThreadLocal.remove();
            for(PrepareCachedEntry<K,V> prepareCachedEntry:map.values()){
                switch (prepareCachedEntry.getOperType()){
                    case Insert:
                    case Update:
                        _map.put(prepareCachedEntry.getKey(),prepareCachedEntry.getData());
                        break;
                    case Delete:
                        _map.remove(prepareCachedEntry.getKey());
                        break;
                }
            }
        }
    }

    @Override
    public void txExceptionFail() {
        // 清空事务缓存
        cachedDataThreadLocal.remove();
    }

    @Override
    public V readNotUpdate(Object key) {
        // 考虑事务：如果事务中有，则取事务中的
        if(inTx()){
            V ret = getFromCache(key);
            if(ret != null){
                return ret;
            }
        }
        return _map.get(key);
    }

    private V getFromCache(Object key){
        PrepareCachedEntry prepareCachedData =  getPrepareDataFromCache(key);
        if(prepareCachedData != null && prepareCachedData.getOperType() != OperType.Delete){
            return (V)prepareCachedData.getData();
        }
        return null;
    }
    private PrepareCachedEntry getPrepareDataFromCache(Object key){
        Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
        if(MapUtils.isNotEmpty(map)){
            PrepareCachedEntry prepareCachedData = map.get(key);
            if(prepareCachedData != null && prepareCachedData.getOperType() != OperType.Delete){
                return prepareCachedData;
            }
        }
        return null;
    }

    @Override
    public V readForUpdate(Object key) {
        V ret = null;
        if(inTx()){
            // 缓存中有，直接返回
            ret = getFromCache(key);
            if(ret != null){
                return ret;
            }
        }

        // 从源拿取
        ret = _map.get(key);
        if(ret == null){
            return null;
        }
        if(inTx()){
            // 深度复制
            ret = EntityHelper.copyEntity(ret);
            // 放入缓存
            putCache(key,ret,OperType.Update); // 这个地方直接协程改，如果是删除的话，会被改成删除
        }
        return ret;
    }

    private void putCache(Object key,V value,OperType operType){
        Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
        if(map == null){
            map = new HashMap<>();
            cachedDataThreadLocal.set(map);
            // 将自己放入事务管理周期
            ContainerService containerService = BeanHelper.getServiceBean(ContainerService.class);
            containerService.registerTxContainer(this);
        }
        PrepareCachedEntry prepareCachedEntry = map.get(key);
        if(prepareCachedEntry == null){
            prepareCachedEntry = new PrepareCachedEntry();
            prepareCachedEntry.setKey(key);
            prepareCachedEntry.setData(value);
            prepareCachedEntry.setOperType(operType);
            //
            map.put((K)key,prepareCachedEntry);
        }else{
            if(prepareCachedEntry.getOperType() == OperType.Delete){
                throw new MMException("not possible!");
            }
            // 关系，Delete>Insert>Update>Select
            switch (operType){
                case Delete:
                case Insert:
                    prepareCachedEntry.setOperType(operType);
                    break;
                case Update:
                    if(prepareCachedEntry.getOperType() == OperType.Select){
                        prepareCachedEntry.setOperType(operType);
                    }
                    break;
            }
        }
    }

    @Override
    public int size() {
        // 考虑事务
        int size = _map.size();
        if(inTx()){
            Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
            if(MapUtils.isNotEmpty(map)){
                for(PrepareCachedEntry prepareCachedEntry:map.values()){
                    if(prepareCachedEntry.getOperType() == OperType.Insert){
                        size++;
                    }else if(prepareCachedEntry.getOperType() == OperType.Delete){
                        size--;
                    }
                }
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        // 考虑事务
        return size()==0;
    }

    @Override
    public boolean containsKey(Object key) {
        //  考虑事务
        if(inTx()){
            PrepareCachedEntry prepareCachedEntry = getPrepareDataFromCache(key);
            if(prepareCachedEntry!= null){
                return prepareCachedEntry.getOperType() != OperType.Delete;
            }
        }
        return _map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        //  考虑事务
        if(inTx()){
            Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
            if(MapUtils.isNotEmpty(map)){
                for(PrepareCachedEntry prepareCachedEntry:map.values()){
                    V v;
                    if((v = (V)prepareCachedEntry.getData())==value || (v!=null && v.equals(value))){
                        return prepareCachedEntry.getOperType() != OperType.Delete;
                    }
                }
            }
        }

        return _map.containsValue(value);
    }

    @Override
    public V put(K key, V value) {
        // 考虑事务
        if(inTx()){
            V ret = readForUpdate(key);
            putCache(key,value,ret == null?OperType.Insert:OperType.Update);
            return ret;
        }
        return _map.put(key,value);
    }

    @Override
    public V remove(Object key) {
        // 考虑事务
        if(inTx()){
            V ret = readForUpdate(key);
            if(ret == null){
                return null;
            }
            putCache(key,ret,OperType.Delete);
            return ret;
        }
        return _map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        //  考虑事务
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()){
            put(e.getKey(),e.getValue());
        }
    }

    @Override
    public void clear() {
        // 考虑事务
        cachedDataThreadLocal.remove();
        _map.clear();
    }

    @Override
    public Set<K> keySet() {
        // 考虑事务
        Set<K> ret = new HashSet<>();
        if(inTx()){
            Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
            if(MapUtils.isNotEmpty(map)){
                for(PrepareCachedEntry prepareCachedEntry:map.values()){
                    if(prepareCachedEntry.getOperType() != OperType.Delete){
                        ret.add((K)prepareCachedEntry.getKey());
                    }
                }
            }
        }
        ret.addAll(_map.keySet());
        return ret;
    }

    /**
     * 事务中不能修改
     * @return
     */
    @Override
    public Collection values() {
        // 考虑事务
        Collection<V> ret = new ArrayList<>();
        if(inTx()){
            Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
            if(MapUtils.isNotEmpty(map)){
                for(PrepareCachedEntry prepareCachedEntry:map.values()){
                    if(prepareCachedEntry.getOperType() != OperType.Delete){
                        ret.add((V)prepareCachedEntry.getData());
                    }
                }
            }
        }
        ret.addAll(_map.values());
        return ret;
    }

    @Override
    public Set<Entry<K,V>> entrySet() {
        // 考虑事务
        Set<Entry<K,V>> ret = new HashSet<>();
        if(inTx()){
            Map<K, PrepareCachedEntry<K,V>> map = cachedDataThreadLocal.get();
            if(MapUtils.isNotEmpty(map)){
                for(PrepareCachedEntry prepareCachedEntry:map.values()){
                    if(prepareCachedEntry.getOperType() != OperType.Delete){
                        ret.add(new MapEntry<>(prepareCachedEntry.getKey(),prepareCachedEntry.getData(),prepareCachedEntry));
                    }
                }
            }
        }
        ret.addAll(_map.entrySet());
        return ret;
    }

    static final class MapEntry<K,V> implements Map.Entry<K,V> {
        final K key; // non-null
        V val;       // non-null
        final PrepareCachedEntry<K,V> prepareCachedEntry;
        MapEntry(K key, V val, PrepareCachedEntry<K,V> prepareCachedEntry) {
            this.key = key;
            this.val = val;
            this.prepareCachedEntry = prepareCachedEntry;
        }
        public K getKey()        { return key; }
        public V getValue()      { return val; }
        public int hashCode()    { return key.hashCode() ^ val.hashCode(); }
        public String toString() { return key + "=" + val; }

        public boolean equals(Object o) {
            Object k, v; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == val || v.equals(val)));
        }

        /**
         * 这里的set，是事务缓存的修改，
         */
        public V setValue(V value) {
            if (value == null) throw new NullPointerException();
            V v = val;
            val = value;
            prepareCachedEntry.setData(value);
            if(prepareCachedEntry.getOperType() == OperType.Select){
                prepareCachedEntry.setOperType(OperType.Update);
            }
            return v;
        }
    }
}
