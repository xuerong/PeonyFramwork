package com.peony.core.security;

public enum MonitorNumType {

//    AllSqlNum("总sql数量"),

    SelectSqlNum("查询sql数量【可缓存数据】"),

    InsertSqlNum("插入sql数量【异步服务】"),
    UpdateSqlNum("更新sql数量【异步服务】"),
    DeleteSqlNum("删除sql数量【异步服务】"),

    ExecuteSelectSqlNum("sql查询数量【DataService直接执行】"),
    ExecuteSqlNum("sql增删改数量【DataService直接执行】"),

    SuccessSqlNum("成功sql数量【异步服务】"),
    FailSqlNum("失败sql数量【异步服务】"),

    CacheHitNum("命中缓存次数"),
    TxCacheHitNum("命中事务缓存次数【仅限查单个值】"),
    CacheNum("缓存数量"),
    CacheEvictedNum("缓存淘汰数量"),

    DoLockSuccessNum("加锁成功次数"),
    DoLockFailNum("加锁失败次数"),
    DoUnLockNum("解锁成功次数"),
    LockerNum("当前锁数量"),

    OnlineUserNum("在线用户数量"),
    RequestNum("用户请求数量"),
    RemoteCallNum("rpc数量"),
    ;
    private final String key;
    MonitorNumType(String key){
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
