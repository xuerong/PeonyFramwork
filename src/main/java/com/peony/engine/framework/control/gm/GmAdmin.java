package com.peony.engine.framework.control.gm;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * GM管理工具是在正式服中（mmserver.properties中的server.is.test设置为true）是需要用户名
 * 和密码才能登陆。TODO 当前，用户名和密码需要手动填写在数据库中。
 *
 * @author zhengyuzhen
 * @see GmService
 * @see Gm
 * @see GmFilter
 * @see GmServlet
 * @see GmSegment
 * @since 1.0
 */
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
