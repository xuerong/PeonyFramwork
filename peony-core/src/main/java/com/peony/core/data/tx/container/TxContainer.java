package com.peony.core.data.tx.container;

import com.peony.core.data.tx.TxCacheService;
import com.peony.core.control.BeanHelper;

public interface TxContainer {
    class Holder{
        private static volatile TxCacheService txCacheService = BeanHelper.getServiceBean(TxCacheService.class);
    }

    default boolean inTx(){
        return Holder.txCacheService.isInTx();
    }

    void txBegin() ;
    void txCommitSuccess();
    void txExceptionFail();
}
