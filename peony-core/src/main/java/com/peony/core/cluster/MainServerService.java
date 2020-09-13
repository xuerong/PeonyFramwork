package com.peony.core.cluster;

import com.peony.core.control.annotation.Service;
import com.peony.core.control.netEvent.NetEventService;
import com.peony.core.data.DataService;
import com.peony.core.data.tx.Tx;
import com.peony.core.server.Server;
import com.peony.common.tool.helper.ConfigHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

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
        Map<String, String> mainServerMap = ConfigHelper.getMap("mainServer");
        if(!"true".equals(mainServerMap.get("mainServer.use"))){
            return;
        }
        ServerInfo mainServerInfo = Server.getEngineConfigure().getMainServerInfo();
        if(Server.getServerId() != mainServerInfo.getId()){ // 非主节点
            return;
        }
        List<ServerInfo> serverInfoList = dataService.selectList(ServerInfo.class,"");
        this.serverInfoList = serverInfoList;
        for(ServerInfo serverInfo : serverInfoList){
            netEventService.registerServerAsync(serverInfo.getId(),serverInfo.getHost(),serverInfo.getNetEventPort());
        }
    }

    /**
     * 根据设备id获取服务器id
     * @param deviceId
     * @return
     */
    public ServerInfo getServerInfoByDeviceId(String deviceId){
        if(StringUtils.isEmpty(deviceId)){
            return null;
        }
        DeviceServer deviceServer = dataService.selectObject(DeviceServer.class,"deviceId = ?",deviceId);
        if(deviceServer == null){
            deviceServer = createDeviceServer(deviceId);
        }
        ServerInfo serverInfo = dataService.selectObject(ServerInfo.class,"id=?",deviceServer.getServerId());
        return serverInfo;
    }
    @Tx
    public DeviceServer createDeviceServer(String deviceId){
        DeviceServer deviceServer = dataService.selectObject(DeviceServer.class,"deviceId = ?",deviceId);
        if(deviceServer == null){
            deviceServer = new DeviceServer();
            deviceServer.setDeviceId(deviceId);
            deviceServer.setRegTime(System.currentTimeMillis());
            deviceServer.setServerId(distributeServer());
            dataService.insert(deviceServer);
        }
        return deviceServer;
    }


    /**
     *
     * TODO  为新用户分配服务器，可用的服务器
     * 0\服务器状态
     * 1、根据热度
     * 2、根据导量配比
     * @return
     */
    public int  distributeServer(){
        return 1;
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
