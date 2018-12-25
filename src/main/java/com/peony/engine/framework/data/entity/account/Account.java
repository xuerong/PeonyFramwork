package com.peony.engine.framework.data.entity.account;

import com.peony.engine.framework.data.entity.session.SessionClient;
import com.peony.engine.framework.data.persistence.orm.annotation.Column;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.persistence.orm.annotation.StringTypeCollation;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by a on 2016/9/18.
 * // TODO 这个的名字和记录的信息需要重新组织一下
 */
@DBEntity(tableName = "account",pks = {"uid"})
public class Account implements SessionClient {

    // accountId,有一定要求,比如要求(字母,数字,下划线,不准有空格,逗号之类的)
    protected String uid;
    @Column(stringColumnType = StringTypeCollation.Varchar128_Mb4)
    protected String name;
    protected String icon;
    protected String clientVersion;//客户端版本号

    protected long createTime;
    protected long lastLoginTime;
    protected long lastLogoutTime;

    // 玩家注册相关信息
    protected int channelId; // 渠道
    protected String deviceId; // 渠道对应的id，如微信，或手机mid，或手机号


    // 玩家登陆统计信息
    protected String area; // 登录地区名称
    protected String country; // 登录国家名称
    protected String device; // 登录设备名称
    protected String deviceSystem; // 登录系统名称
    protected String networkType; // 联网类型名称
    protected String prisonBreak; // 是否越狱(0:否 1:是)
    protected String operator; // 运营商名称

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public long getLastLogoutTime() {
        return lastLogoutTime;
    }

    public void setLastLogoutTime(long lastLogoutTime) {
        this.lastLogoutTime = lastLogoutTime;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getDeviceSystem() {
        return deviceSystem;
    }

    public void setDeviceSystem(String deviceSystem) {
        this.deviceSystem = deviceSystem;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getPrisonBreak() {
        return prisonBreak;
    }

    public void setPrisonBreak(String prisonBreak) {
        this.prisonBreak = prisonBreak;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }



    @Override
    public void destroySession() {
        // 下线通知
    }

    public void logout(LogoutReason reason){
        switch (reason){
            case userLogout:
                break;
            case replaceLogout: // 通知前端
                break;
            case netErrorLogout: // 通知前端
                break;
        }
    }
}
