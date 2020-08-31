package com.peony.core.data.cache;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/11/24.
 *
 * 这里面的数据可以考虑不用返回(flush)而是出现错误抛出异常
 */
public interface CacheCenter {
    /**
     * 存入新的CacheEntity，返回缓存是否成功
     * 如果缓存中已经存在，返回false
     * */
    public CacheEntity putIfAbsent(String key,CacheEntity entity);

    /**
     * 缓存一个列表的数据
     * 有则不修改
     * @param entityMap
     */
    public void putList(Map<String,CacheEntity> entityMap);
    /**
     * 获取
     * */
    public CacheEntity get(String key);
    /**
     * 获取多个值
     * ##################这里要求获取到的list和keys顺序是对应的################
     */
    public List<CacheEntity> getList(String... keys);
    /**
     * 移除
     * */
    public CacheEntity remove(String key);
    /**
     *  更新,不考虑版本
     *  不用cas,因为cas失败了会导致事务无法回退,用的是先所有的加锁之后校验,
     * */
    public Object update(String key,CacheEntity entity);

    default public int size()  {
        return -1;
    }
    default public int evictNum(){
        return -1;
    }
}
