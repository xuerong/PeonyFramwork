package com.peony.core.data.entity.account;

import com.peony.core.data.entity.session.Session;

/**
 * Created by a on 2016/9/18.
 */
public class LogoutEventData {
    private Session session;
    private LogoutReason logoutReason;

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public LogoutReason getLogoutReason() {
        return logoutReason;
    }

    public void setLogoutReason(LogoutReason logoutReason) {
        this.logoutReason = logoutReason;
    }
}
