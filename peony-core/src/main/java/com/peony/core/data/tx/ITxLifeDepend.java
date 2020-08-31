package com.peony.core.data.tx;

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
