package com.peony.core.cluster;

import com.peony.core.control.annotation.EventListener;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.event.EventService;
import com.peony.core.control.gm.Gm;
import com.peony.core.control.job.JobService;
import com.peony.core.control.netEvent.NetEventService;
import com.peony.core.control.rpc.Remotable;
import com.peony.core.control.rpc.RouteType;
import com.peony.core.data.DataService;
import com.peony.core.data.entity.account.sendMessage.SendMessageService;
import com.peony.core.data.tx.Tx;
import com.peony.core.security.MonitorService;
import com.peony.core.server.Server;
import com.peony.common.tool.helper.ConfigHelper;

import java.io.Serializable;
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
    private SendMessageService sendMessageService;
    private JobService jobService;

    public void init(){
        Map<String, String> mainServerMap = ConfigHelper.getMap("mainServer");
        if(!mainServerMap.get("mainServer.use").trim().equals("true")){
            return;
        }
        ServerInfo mainServerInfo = Server.getEngineConfigure().getMainServerInfo();
        if(Server.getServerId() == mainServerInfo.getId()){ // 主节点
            return;
        }
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

    @Tx
    @Remotable(route = RouteType.SERVERID,routeArgIndex = 1)
    public void aaa(int serverId){
        System.out.println("in  --");
    }

    private EventService eventService;

    @Tx
    @Gm(id="test remotable tx")
    public void testGm() throws Exception{
//        Object serviceBean = BeanHelper.getServiceBean(NodeServerService.class.getName());
//        Method method = serviceBean.getClass().getMethod("aaa",int.class);
//        method.invoke(serviceBean,1);
//        aaa(1);
//        eventService.fireEvent(null,12122);
//        adadad(4);
//        sendMessageService.sendMessage("sdf",92929,new JSONObject());

//        UserBase userBase = new UserBase();
//        userBase.setIcon("sdfsdfsdf");
//        userBase.setUid("ffffff");
//        userBase.setLevel(10);
//        Map<String,Object> map = EntityHelper.getEntityParser(UserBase.class).toMap(userBase);
//        System.out.println(map);

//        UserBase userBase = dataService.selectObject(UserBase.class,"uid=?","zyz02");
//        userBase.setLevel(123);
//        dataService.update(userBase);
//
//        UserBase userBase1 = new UserBase();
//        userBase1.setUid("testnewEntity");
//        userBase1.setGold(100);
//        dataService.insert(userBase1);

        jobService.startJob(5000,NodeServerService.class,"adadad","sdfsdfs");
    }
    class A implements Serializable{
        int a = 10;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }
    }
    @Tx
    @EventListener(event = 12122)
    public void adadad(String a){
        System.out.println(a);
    }
}
