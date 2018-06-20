package com.peony.engine.framework.data.cache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;
import com.peony.engine.framework.server.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2017/6/29.
 */
public class LRUMapCacheCenter implements CacheCenter {
    AtomicInteger evictedNum = new AtomicInteger(0);
    //    Concurrentlinkedh
    ConcurrentLinkedHashMap<String, CacheEntity> map = new ConcurrentLinkedHashMap.Builder<String, CacheEntity>()
            .maximumWeightedCapacity(Integer.parseInt(Server.getEngineConfigure().getString("maximumWeightedCapacity")))
            .weigher(Weighers.singleton()).listener((key, value) -> {
                evictedNum.getAndIncrement();
            })
            .build();

    //    ConcurrentHashMap<String,CacheEntity> map = new ConcurrentHashMap<>();
    @Override
    public CacheEntity putIfAbsent(String key, CacheEntity entity) {
        return map.putIfAbsent(key, entity);
    }

    @Override
    public void putList(Map<String, CacheEntity> entityMap) {
        map.putAll(entityMap);
    }

    @Override
    public CacheEntity get(String key) {
        CacheEntity ret = map.get(key);
        if(ret != null){
            ret = ret.clone();
        }
        return ret;
    }

    @Override
    public List<CacheEntity> getList(String... keys) {
        List<CacheEntity> result = new ArrayList<>();
        for (String key : keys) {
            CacheEntity cacheEntity = get(key);
            if (cacheEntity == null) {
                return null;
            }
            result.add(cacheEntity);
        }
        return result;
    }

    @Override
    public CacheEntity remove(String key) {
        return map.remove(key);
    }

    @Override
    public boolean update(String key, CacheEntity entity) {
        map.put(key, entity);
        return true;
    }

    @Override
    public int size()  {
        return map.size();
    }
    @Override
    public int evictNum(){
        return evictedNum.get();
    }
}
