package com.peony.engine.framework.control.netEvent;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.ServerType;

import java.util.List;

/**
 * 主服务器服务：serverlist服务器服务
 *
 * 获取服务器列表，并缓存
 * 向NetEvent注册所有的服务器
 *
 */
@Service(init="init")
public class MainServerService {

    private DataService dataService;
    private NetEventService netEventService;

    private List<ServerInfo> serverInfoList;

    public void init(){
        List<ServerInfo> serverInfoList = dataService.selectList(ServerInfo.class,"");
        this.serverInfoList = serverInfoList;
        for(ServerInfo serverInfo : serverInfoList){
            netEventService.registerServerAsync(serverInfo.getId(),serverInfo.getHost(),serverInfo.getNetEventPort());
        }
    }

    /**
     * 向主服务器发送事件
     * 异步
     */
//    public void fireMainServerNetEvent(NetEventData netEvent) {
//        if (ServerType.isMainServer()) {
//            handleNetEventData(netEvent);
//            return;
//        }
//        if (mainServerClient != null) {
//            mainServerClient.push(netEvent);
//            return;
//        }
//        throw new MMException("mainServerClient is null");
//    }
}
