package com.peony.core.data.cache;

import com.peony.core.control.annotation.NetEventListener;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.netEvent.NetEventData;
import com.peony.core.control.netEvent.NetEventService;
import com.peony.core.server.SysConstantDefine;
import com.peony.core.control.BeanHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/11/24.
 * CacheCenterImpl中的本地缓存使用ehcache，公共缓存使用memcached
 *
 * 本地缓存和公共缓存都要做成插件式的
 *
 * 添加的时候，先添加远程，再添加本地（还是flush？如果flush，那么第一个get并放进远程的时候，不会放进local），如果远程已存在，用已存在的更新本地（还是flush？）
 * 获取的时候，先本地获取，没有则远程获取，并更新到本地
 * 删除的时候，先flush本地，再删除远程
 * 更新的时候，先flush本地，再更新远程(cas)
 *
 * 问题：
 * 1 别人更新了远程，需要通知自己flush本地
 *
 */
@Service(init = "init")
public class CacheCenterImpl implements CacheCenter {
    /**
     * 添加新数据时，先发送远程缓存，再更新本地，确保远程的一定比本地的新
     *
     * 在这一层要对缓存中出现的失败进行基本的处理，如打印日志
     *
     * */
    private NetEventService netEventService;
    public void init(){
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
    }

    @Override
    public CacheEntity putIfAbsent(String key,CacheEntity entity) {
        CacheEntity older = MemCachedHelper.putIfAbsent(key,entity);
        if(older != null){ // 说明里面已经有了，可能是另外一个线程放入的
            EhCacheHelper.put(key,older);
            return older;
        }
        EhCacheHelper.put(key,entity);
        return null;
    }

    @Override
    public void putList(Map<String,CacheEntity> entityMap){
        MemCachedHelper.putList(entityMap);
        EhCacheHelper.putList(entityMap);
        return ;
    }

    /**
     * 从缓存中获取数据
     * 如果本地存在，返回本地，否则返回公共缓存的，否则，返回null
     * */
    @Override
    public CacheEntity get(String key) {
        CacheEntity entity=EhCacheHelper.get(key);
        if(entity!=null){
            return entity;
        }
        entity = MemCachedHelper.get(key);
        if(entity!=null){
            // 放在本地缓存
            if(!EhCacheHelper.put(key,entity)){
                // 缓存本地失败
            }
        }
        return entity;
    }
    @Override
    public List<CacheEntity> getList(String... keys){
        List<CacheEntity> result = EhCacheHelper.getList(keys);
        if(result != null){
            return result;
        }
        result = MemCachedHelper.getList(keys);
        if(result != null && result.size() > 0){
            // 这个是否放入本地,或许可以不
            Map<String,CacheEntity> map = new HashMap<>();
            int length = keys.length;
            for(int i=0;i<length;i++){
                map.put(keys[i],result.get(i));
            }
            if(!EhCacheHelper.putList(map)){
                // 缓存本地失败
            }
        }
        return result;
    }
    /**
     * 从本地缓存中移除，并从公共缓存中移除
     *
     * 注意，这个显然是移除数据，不是删除数据库数据，删除数据库数据，应该是在缓存中对该对象加删除标记
     * */
    @Override
    public CacheEntity remove(String key) {
        EhCacheHelper.remove(key);
        MemCachedHelper.remove(key);
        return null;
    }

    /**
     * 重新保存数据
     * 缓存中的数据不需要变动，因为是引用
     * 更新memcached中的变动数据
     * 不用cas了
     * */
    @Override
    public Object update(String key,CacheEntity entity) {
        EhCacheHelper.update(key,entity);
        MemCachedHelper.update(key,entity);

        // 广播给其它服务器,使其更新缓存
        broadcastUpdateCache(key);
        return true;
    }

    private void broadcastUpdateCache(String key){
        NetEventData eventData = new NetEventData(SysConstantDefine.CACHEUPDATE);
        eventData.setParam(key);
        netEventService.broadcastNetEvent(eventData,false);
    }
    // TODO 这里还没有处理
    @NetEventListener(netEvent = SysConstantDefine.CACHEUPDATE)
    public NetEventData updateCacheListener(NetEventData eventData){
        return new NetEventData(eventData.getNetEvent(),eventData.getParam());
    }
}
