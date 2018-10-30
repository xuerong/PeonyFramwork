package com.peony.engine.framework.cluster;

import com.peony.engine.framework.security.exception.MMException;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by a on 2016/9/18.
 * nodeServer的状态
 */
public class NodeServerState {

    private ServerInfo serverInfo;

    private int workload; // 负载
    private int accountCount; // 账户数量

    private Set<String> accountIdSet = new HashSet<>();

    public String getKey(){
        return serverInfo.getHost()+":"+serverInfo.getRequestPort();
    }
    public String getNetEventAdd(){
        return serverInfo.getHost()+":"+serverInfo.getNetEventPort();
    }

    public synchronized void addAccount(String accountId){
        boolean newOne = accountIdSet.add(accountId);
        if(newOne){
            accountCount ++;
            workload++;
        }
    }
    public synchronized void removeAccount(String accountId){
        if(accountCount<=0 || workload<=0 ){
            throw new MMException("accountCount < 0");
        }
        boolean has = accountIdSet.remove(accountId);
        if(has){
            accountCount --;
            workload --;
        }
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public Set<String> getAccountIdSet() {
        return accountIdSet;
    }

    public int getWorkload() {
        return workload;
    }

    public void setWorkload(int workload) {
        this.workload = workload;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }
}
