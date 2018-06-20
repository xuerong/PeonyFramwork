package com.peony.engine.framework.control.netEvent;

import io.netty.channel.Channel;

import java.io.Serializable;

/**
 * Created by Administrator on 2015/12/30.
 */
public class NetEventData implements Serializable{
    private int netEvent;
    private int errorCode; // 错误码
    private Object param;
    private transient Channel channel;
    public NetEventData(int netEvent,Object param){
        this.netEvent = netEvent;
        this.param = param;
    }
    public NetEventData(int netEvent){
        this.netEvent=netEvent;
    }

    public int getNetEvent(){
        return netEvent;
    }

    public void setNetEvent(int netEvent) {
        this.netEvent = netEvent;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
