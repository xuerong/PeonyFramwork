package com.peony.engine.framework.data.entity.session;

import com.peony.engine.framework.data.entity.account.LogoutReason;

/**
 * Created by a on 2016/11/3.
 */
public interface ConnectionClose {
    public void close(LogoutReason logoutReason);
}
