package com.peony.engine.framework.data.cache;

import java.util.List;
import java.util.Map;

public interface IRemoteCacheClient {

	/**
	 * 初始化cache
	 */
	public void init();

	/**
	 * 关闭cache
	 */
	public void close();

	public boolean set(String key, int exp, Object obj);
	/*
	 * set 不等待
	 * */
	public void setWithNoReply(String key, int exp, Object obj);	

	/**
	 * 增加到缓存，如果已经存在，返回的future.get()为false，否则为true; 用于做同步锁
	 * 
	 * @param key
	 * @param exp
	 * @param obj
	 * @return
	 */
	public boolean add(String key, int exp, Object obj);

	public Object get(String key);

	public boolean delete(String key);
	/**
	 * 删除，不等待相应
	 */
	public void deleteWithNoReply(String key);	

	public Map<String, Object> getBulk(String[] keys);
	public Map<String, Object> getBulk(List<String> keys);

	public long incr(String key, int by);

	public long decr(String key, int by);

	public void flush();
}
