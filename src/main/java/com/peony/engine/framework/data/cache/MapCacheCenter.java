package com.peony.engine.framework.data.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/6/29.
 */
public class MapCacheCenter implements CacheCenter {
    ConcurrentHashMap<String,CacheEntity> map = new ConcurrentHashMap<>();
    @Override
    public CacheEntity putIfAbsent(String key, CacheEntity entity) {
        return map.putIfAbsent(key,entity);
    }

    @Override
    public void putList(Map<String, CacheEntity> entityMap) {
        map.putAll(entityMap);
    }

    @Override
    public CacheEntity get(String key) {
        return map.get(key);
    }

    @Override
    public List<CacheEntity> getList(String... keys) {
        List<CacheEntity> result = new ArrayList<>();
        for(String key : keys){
            CacheEntity cacheEntity = map.get(key);
            if(cacheEntity == null){
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
    public Object update(String key, CacheEntity entity) {
        return map.put(key,entity);
    }
}
