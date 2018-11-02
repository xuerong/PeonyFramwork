package com.peony.engine.framework.data.entity.account;

import com.peony.engine.framework.data.entity.session.ConnectionClose;
import io.netty.channel.ChannelHandlerContext;

/**
 * 登录的时候需要传的信息，这个在不同的协议的登录消息中构建，然后传给AccountSysService
 */

public class LoginInfo {
    private String deviceId;
    private String uid;
    private String name;
    private String url;
    private String ip;
    private Object loginParams;
    private MessageSender messageSender;
    private String appversion;
    private String country;
    private String localization;

    private ChannelHandlerContext ctx; // 如果是长连接，这个是有的，如果是短连接，可以为空

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Object getLoginParams() {
        return loginParams;
    }

    public void setLoginParams(Object loginParams) {
        this.loginParams = loginParams;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public String getAppversion() {
        return appversion;
    }

    public void setAppversion(String appversion) {
        this.appversion = appversion;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLocalization() {
        return localization;
    }

    public void setLocalization(String localization) {
        this.localization = localization;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
}
