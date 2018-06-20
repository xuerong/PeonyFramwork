package com.peony.engine.framework.control.netEvent;

import java.io.Serializable;

/**
 * Created by a on 2016/8/31.
 */
public class ServerInfo implements Serializable {
    private int id;
    private String name;
    private String innerHost;
    private String publicHost;
    private int netEventPort;
    private int requestPort;
    private int type;

    private int verifyServer; // 是否是审核服

    private int accountCount;
    private int hot; // 火爆程度，根据最近的登陆情况计算
    private int state; // 状态

    public ServerInfo(){

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return this.id;
    }

    public String getInnerAddress(){
        return new StringBuilder(innerHost).append(":").append(netEventPort).toString();
    }

    @Override
    public String toString(){
        return new StringBuilder(innerHost).append(":").append(netEventPort).toString();
    }

    public String getHost() {
        return innerHost;
    }

    public void setHost(String host) {
        this.innerHost = host;
    }

    public String getPublicHost() {
        return publicHost;
    }

    public void setPublicHost(String publicHost) {
        this.publicHost = publicHost;
    }

    public int getNetEventPort() {
        return netEventPort;
    }

    public void setNetEventPort(int netEventPort) {
        this.netEventPort = netEventPort;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getRequestPort() {
        return requestPort;
    }

    public void setRequestPort(int requestPort) {
        this.requestPort = requestPort;
    }


    public String getInnerHost() {
        return innerHost;
    }

    public void setInnerHost(String innerHost) {
        this.innerHost = innerHost;
    }

    public int getVerifyServer() {
        return verifyServer;
    }

    public void setVerifyServer(int verifyServer) {
        this.verifyServer = verifyServer;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public int getHot() {
        return hot;
    }

    public void setHot(int hot) {
        this.hot = hot;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
