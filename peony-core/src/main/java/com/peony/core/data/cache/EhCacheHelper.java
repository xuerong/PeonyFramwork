package com.peony.core.data.cache;

import com.peony.core.server.Server;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

import java.util.*;

/**
 * Created by Administrator on 2015/11/24.
 * TODO 这个怎么做淘汰?
 */
public class EhCacheHelper {
    private static final Map<String, Cache<String,CacheEntity>> cacheMap=new HashMap<>();
    private static final CacheManager cacheManager;

    static{
        // 根据继承自CacheEntity创建缓存组
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
    }
    /**
     * 添加数据，并返回相应的key
     * */
    public static boolean put(String key,CacheEntity entity) {
        Cache<String, CacheEntity> cache=getCache(entity);
        cache.put(key,entity);
        return true;
    }
    public static boolean putList(Map<String,CacheEntity> entityMap) {
        for(Map.Entry<String,CacheEntity> entry : entityMap.entrySet()){
            Cache<String, CacheEntity> cache=getCache(null);
            cache.put(entry.getKey(),entry.getValue());
        }
        return true;
    }

    public static CacheEntity get(String key) {
        Cache<String, CacheEntity> cache=getCache(null);
        return cache.get(key);
    }

    public static List<CacheEntity> getList(String... keys){
        Cache<String, CacheEntity> cache=getCache(null);
        List<CacheEntity> result = null;
        for(String key : keys){
            CacheEntity cacheEntity = cache.get(key);
            if(cacheEntity == null){ // 只要有一个不存在,就不存在
                return null;
            }
            if(result == null){
                result = new ArrayList<>();
            }
            result.add(cacheEntity);
        }
        return result;
    }

    public static boolean remove(String key) {
        Cache<String, CacheEntity> cache=getCache(null);
        cache.remove(key);
        return true;
    }

    public static boolean update(String key,CacheEntity entity) { // 更新时删除本地的缓存数据，防止数据不一致
//        Cache<String, Object> cache=getCache(entity);
//        cache.insert(key,entity);
        return remove(key);
    }

    private static Cache<String,CacheEntity> getCache(CacheEntity entity){
        String cacheKey = "cacheKey";//entity.getClass().getName();
        Cache<String, CacheEntity> cache=cacheMap.get(cacheKey);
        if(cache == null){
            synchronized (cacheMap){
                cache=cacheMap.get(cacheKey);
                if(cache == null){
                    // FIXME 这个设置需要考虑一下
                    cache = cacheManager.createCache(cacheKey,
                            CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, CacheEntity.class, ResourcePoolsBuilder.heap(Server.getEngineConfigure().getInteger("cacheCapacity",3000000))).build());
                    cacheMap.put(cacheKey,cache);
                }
            }
        }
        return cache;
    }
}