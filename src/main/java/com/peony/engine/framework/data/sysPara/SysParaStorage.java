package com.peony.engine.framework.data.sysPara;

import java.util.Map;

/**
 * Created by a on 2016/9/27.
 * 这个是用来存储系统变量的,包括:
 * 1、修改的策划配数
 * 2、非策划配数
 */
public interface SysParaStorage {
    public Map<String,String> getAllSysPara();
    public void insertSysPara(String key,String value);
    public void update(String key,String value);
    public void delete(String key);
}
