package com.peony.core.data.entity.session;

import com.peony.core.data.cache.CacheEntity;
import com.peony.core.data.entity.account.LogoutReason;
import com.peony.core.data.entity.account.MessageSender;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/11/16.
 * Session中封装了和客户端访问相关的数据，是对所有访问种类@class EntranceType的统一封装：
 * 如，访问种类，访问url，访问port，访问者ip等信息
 *
 * 每一个Request都包含一个Session，
 *
 * TODO session放缓存的时候是不是需要把它作为CacheEntity的object，而不是继承自它
 */

public class Session extends CacheEntity{
    private String url;
    // sessionid的组成包括两部分，一是前缀，用来记录和登陆相关的一些信息，二是cacheEntity的id
    private final String sessionId;
    // session所对应的客户端,这个当客户端登陆的时候赋值,以便在后续的使用中从session中找到它
//    private SessionClient sessionClient;
    private String uid;
    private final String ip;
    private final long createTime;
    private Date lastUpdateTime;
    private MessageSender messageSender;
    private ConnectionClose connectionClose;
    // 属性
    private Map<String,Object> attrs;
    // 是否可用，当登出的时候，设置成false，应为有可能被其它功能引用
    private boolean available;
    //
    private String localization; // 语言，本地化用
    //
    private boolean newUser; // 新用户

    public Session(String url, String sessionIdPrefix, String ip){
        this.url=url;
        //"Session_"+sessionIdPrefix;
        this.sessionId="Session_"+sessionIdPrefix;
        this.ip=ip;
        this.createTime= System.currentTimeMillis();
        this.lastUpdateTime= new Date(createTime);
        attrs = new ConcurrentHashMap<>();
        available = true;
    }

    public Object getAttr(String key){
        return attrs.get(key);
    }
    public void setAttr(String key,Object object){
        this.attrs.put(key,object);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getIp() {
        return ip;
    }

    public long getCreateTime() {
        return createTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public void setExpired() {
        lastUpdateTime.setTime(0);
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }


    public ConnectionClose getConnectionClose() {
        return connectionClose;
    }

    public void setConnectionClose(ConnectionClose connectionClose) {
        this.connectionClose = connectionClose;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void closeConnect(LogoutReason logoutReason){
        if(this.connectionClose != null){
            this.connectionClose.close(logoutReason);
        }
    }

    public String getLocalization() {
        return localization;
    }

    public void setLocalization(String localization) {
        this.localization = localization;
    }

    public boolean isNewUser() {
        return newUser;
    }

    public void setNewUser(boolean newUser) {
        this.newUser = newUser;
    }
}
