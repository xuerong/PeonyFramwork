package com.peony.engine.framework.data.entity.account;

import io.netty.channel.ChannelHandlerContext;

/**
 * 登录的时候需要传的信息，这个在不同的协议的登录消息中构建，然后传给AccountSysService
 */

public class LoginInfo {
    private ChannelHandlerContext ctx;
    private String id;
    private String name;
    private String url;
    private String ip;
    private Object loginParams;
    private MessageSender messageSender;

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
