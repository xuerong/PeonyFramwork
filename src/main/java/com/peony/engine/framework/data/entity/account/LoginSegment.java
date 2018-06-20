package com.peony.engine.framework.data.entity.account;

import com.peony.engine.framework.data.entity.session.Session;

/**
 * Created by a on 2016/9/18.
 */
public class LoginSegment {
    private Session session;
    private Account account;

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
