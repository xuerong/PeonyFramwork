package com.peony.engine.framework.control.gm;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "gmadmin",pks = {"account"})
public class GmAdmin implements Serializable{
    private String account;
    private String password;

    private long lastLogin;

    // todo 后续可以添加更多，如操作记录

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
}
