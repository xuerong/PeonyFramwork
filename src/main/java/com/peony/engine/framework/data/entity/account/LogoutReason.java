package com.peony.engine.framework.data.entity.account;

/**
 * Created by a on 2016/9/18.
 * 登出原因
 */
public enum LogoutReason {
    userLogout, // 从mainServer传过来，清除session信息
    replaceLogout, // 从mainServer传过来，清除session信息
    netErrorLogout,
    netDisconnect, // nodeServer断连,通知mainServer，并清除session信息
    sessionOutTime,
    CloseServer,
    HeartOutTime // 心跳超时
}
