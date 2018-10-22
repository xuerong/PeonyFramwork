package com.peony.engine.framework.control.netEvent;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.rpc.Remotable;
import com.peony.engine.framework.control.rpc.RouteType;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.ConfigHelper;

import java.util.List;
import java.util.Map;

/**
 * 向NetEvent同步注册mainserver
 * 远程调用mainServer，获取服务器列表
 * 向NetEvent注册各个服务器
 */
@Service(init = "init")
public class NodeServerService {

    private MonitorService monitorService;
    private NetEventService netEventService;
    private DataService dataService;

    public void init(){
        Map<String, String> mainServerMap = ConfigHelper.getMap("mainServer");
        if(!mainServerMap.get("mainServer.use").trim().equals("true")){
            return;
        }
        ServerInfo mainServerInfo = Server.getEngineConfigure().getMainServerInfo();
        netEventService.registerServerSyn(mainServerInfo.getId(),mainServerInfo.getHost(),mainServerInfo.getNetEventPort());
        List<ServerInfo> serverInfoList = getServerInfoList(mainServerInfo.getId());
        for(ServerInfo serverInfo : serverInfoList){
            netEventService.registerServerAsync(serverInfo.getId(),serverInfo.getHost(),serverInfo.getNetEventPort());
        }
    }

    @Remotable(route = RouteType.SERVERID,routeArgIndex = 1)
    public List<ServerInfo> getServerInfoList(int serverId){
        List<ServerInfo> serverInfoList = dataService.selectList(ServerInfo.class,"");
        return serverInfoList;
    }
}
