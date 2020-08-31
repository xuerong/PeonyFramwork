package com.peony.core.data.tx;

import com.peony.core.control.BeanHelper;

/**
 * @Author: zhengyuzhen
 * @Date: 2020-08-30 14:28
 */
final class TxCacheServiceHolder{
    public static final TxCacheService txCacheService = BeanHelper.getServiceBean(TxCacheService.class);
}
