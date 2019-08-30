package com.peony.engine.framework.data.tx.container;

import com.peony.engine.framework.data.tx.TxCacheService;
import com.peony.engine.framework.tool.helper.BeanHelper;

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
