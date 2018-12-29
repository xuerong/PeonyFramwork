package com.peony.engine.framework.cluster;

import com.myFruit.game.userBase.UserBase;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.control.netEvent.NetEventService;
import com.peony.engine.framework.control.rpc.Remotable;
import com.peony.engine.framework.control.rpc.RouteType;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.persistence.orm.EntityHelper;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.helper.ConfigHelper;

import java.lang.reflect.Method;
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

    @Tx
    @Remotable(route = RouteType.SERVERID,routeArgIndex = 1)
    public void aaa(int serverId){
        System.out.println("in  --");
    }

    private EventService eventService;

    @Gm(id="test remotable tx")
    public void testGm() throws Exception{
//        Object serviceBean = BeanHelper.getServiceBean(NodeServerService.class.getName());
//        Method method = serviceBean.getClass().getMethod("aaa",int.class);
//        method.invoke(serviceBean,1);
//        aaa(1);
//        eventService.fireEvent(null,12122);
//        adadad(4);

//        UserBase userBase = new UserBase();
//        userBase.setIcon("sdfsdfsdf");
//        userBase.setUid("ffffff");
//        userBase.setLevel(10);
//        Map<String,Object> map = EntityHelper.getEntityParser(UserBase.class).toMap(userBase);
//        System.out.println(map);
        UserBase userBase = dataService.selectObject(UserBase.class,"uid=?","zyz02");
        userBase.setLevel(123);
        dataService.update(userBase);

        UserBase userBase1 = new UserBase();
        userBase1.setUid("testnewEntity");
        userBase1.setGold(100);
        dataService.insert(userBase1);
    }
    class A{
        int a = 10;
    }
    @EventListener(event = 12122)
    public void adadad(A a){
        System.out.println(a);
    }
}
