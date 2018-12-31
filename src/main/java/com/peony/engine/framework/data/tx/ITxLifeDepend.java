package com.peony.engine.framework.data.tx;

import com.peony.engine.framework.tool.helper.BeanHelper;

public interface ITxLifeDepend {
    default boolean isInTx(){
        return TxCacheServiceHolder.txCacheService.isInTx();
    }
    // 事务开始
    // 事务提交
    // 事务异常
    default void txBegin() {

    };
    default void txCommitSuccess(){

    };
    default void txExceptionFail(){

    };
}
final class TxCacheServiceHolder{
    public static final TxCacheService txCacheService = BeanHelper.getServiceBean(TxCacheService.class);
}
