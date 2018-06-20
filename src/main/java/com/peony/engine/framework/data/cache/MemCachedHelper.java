package com.peony.engine.framework.data.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/11/24.
 */
public class MemCachedHelper {
    private static final int exp = 10000;
    private static final Logger log = LoggerFactory.getLogger(MemCachedHelper.class);
    private static final IRemoteCacheClient remoteCacheClient;
    static {
        XMemCached xMemCached = new XMemCached();
        xMemCached.setServerlist("localhost:11211");
        xMemCached.setOpTimeout(5000);
        xMemCached.setReadBufSize(65535);
//        xMemCached.setTranscoder();

        remoteCacheClient = xMemCached;
        remoteCacheClient.init();
    }

    public static CacheEntity get(String key){
        Object object = remoteCacheClient.get(key);
        if(object!= null && object instanceof CacheEntity){
            return (CacheEntity)object;
        }
        return null;
    }

    public static List<CacheEntity> getList(String... keys){
        Map<String, Object> map = remoteCacheClient.getBulk(keys);
        if(keys == null || keys.length == 0){
            return null;
        }
        if(map == null || map.size() == 0){
            return null;
        }
        List<CacheEntity> result = new ArrayList<>(keys.length);
        for(String key : keys){
            result.add((CacheEntity) map.get(key));
        }
        return result;
    }

    public static void remove(String key){
        if(!remoteCacheClient.delete(key)){
            // TODO 这种地方如何处理
            log.error("删除memcached数据失败，key = "+key);
        }
    }

    public static CacheEntity putIfAbsent(String key,CacheEntity entity){
        boolean success = remoteCacheClient.add(key,exp,entity);
        if(success){
            return null;
        }
        // 这个不一定是add的时候里面有的，但是是现在有的
        return (CacheEntity)remoteCacheClient.get(key);
    }
    public static void putList(Map<String,CacheEntity> entityMap){
        // TODO 这个有什么办法可以提高效率，用线程池？
        for(Map.Entry<String,CacheEntity> entry : entityMap.entrySet()){
//            remoteCacheClient.setWithNoReply(entry.getKey(),exp,entry.getValue());
            // setWithNoReply会导致，数据还未完全存储就返回了
            remoteCacheClient.set(entry.getKey(),exp,entry.getValue());
        }
    }
    public static void update(String key,CacheEntity entity){
        remoteCacheClient.set(key,exp,entity);
    }
}
