package com.peony.engine.framework.control.netEvent;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.NetEventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.net.entrance.Entrance;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.ServerType;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.server.configure.EntranceConfigure;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.util.Util;
import com.sun.tools.jdi.InternalEventHandler;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by Administrator on 2015/12/30.
 * <p>
 * TODO 发送netEvent有两种情况：
 * 1、自己执行了，接收者如果是自己，就不用发，或者不用处理：本地缓存之间的flush通知
 * 2、自己没有执行，接受者是谁都要处理：加锁
 * <p>
 * 广播默认不发给自己
 * 发给单个服务器的，要发给自己
 * 自己不连自己，而是直接调用
 * TODO 被忘了设定超时机制
 * TODO 调用的方法命名有点混乱
 */
@Service(init = "init",initPriority = 1)
public class NetEventService {
    private static final Logger logger = LoggerFactory.getLogger(NetEventService.class);

    private static final String SERVERSKEY = "servers";
    private static int processors = Runtime.getRuntime().availableProcessors();

    private Map<Integer, NetEventListenerHandler> handlerMap = null;
    // 最多平均每个线程有10个请求等待处理
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            processors * 5, processors * 10, 5, TimeUnit.MINUTES, new LinkedBlockingDeque<>(processors * 100),
            (r, executor) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("NetEventService is busy {}", executor.toString());
                }
                logger.warn("NetEventService is busy drop task");
            });

    // 所有已经添加的服务器
    private Map<Integer, ServerInfo> serverInfos = new ConcurrentHashMap<>();
    // 所有的serverClient 不包括自己 TODO 一个server可能既是这个server又是那个server
    private Map<Integer, ServerClient> serverClients = new ConcurrentHashMap<>();

    // mainServer 不包括自己
    private ServerClient asyncServerClient;

    // mainServer client 不包括自己
    private ServerClient mainServerClient;
    //
    private String selfAddress;
    private ServerInfo selfServerInfo;
    //
    private MonitorService monitorService;

    private EventService eventService;

    public void init() {
        handlerMap = new HashMap<>();
        TIntObjectHashMap<Class<?>> netEventHandlerClassMap = ServiceHelper.getNetEventListenerHandlerClassMap();
        netEventHandlerClassMap.forEachEntry((i, aClass) -> {
            handlerMap.put(i, (NetEventListenerHandler) BeanHelper.getServiceBean(aClass));
            return true;
        });

        selfAddress = getServerKey(Util.getHostAddress(), Server.getEngineConfigure().getNetEventPort());
        System.err.println("selfAddress "+selfAddress);
        // TODO 初始化自己的信息
        ServerInfo serverInfo = new ServerInfo();
        int id = Server.getEngineConfigure().getInteger("serverId");
        serverInfo.setId(id);
        serverInfo.setHost(Util.getHostAddress());
        serverInfo.setNetEventPort(Server.getEngineConfigure().getNetEventPort());
        serverInfo.setType(ServerType.getServerType());
        serverInfo.setRequestPort(Server.getEngineConfigure().getRequestPort());

        selfServerInfo = serverInfo;

        monitorService.addStartCondition(SysConstantDefine.NetEventServiceStart,
                "wait for netEvent start and connect mainServer");
    }

    private String getServerKey(String host, int port) {
        return host + ":" + port;
    }

    @NetEventListener(netEvent = SysConstantDefine.NETEVENT_PONG)
    public NetEventData pong(NetEventData eventData) {
        logger.info("receive poing from {}", eventData.getChannel());
        return null;
    }

    @EventListener(event = SysConstantDefine.Event_EntranceStart)
    public void entranceStart(EventData eventData) {
        Entrance entrance = (Entrance) eventData.getData();
        EntranceConfigure entranceConfigure = Server.getEngineConfigure().getNetEventEntrance();
        if (entranceConfigure.getName().equals(entrance.getName())) {
            // netEvent入口已经启动
            notifyConnMainServer();
            monitorService.removeStartCondition(SysConstantDefine.NetEventServiceStart);
        }
    }

    public void notifyConnMainServer() {
        if (ServerType.isMainServer()) {
            logger.info("不需要连接mainServer,本服务器即为mainServer");
            return;
        }
        String mainServerAdd = Server.getEngineConfigure().getMainServerNetEventAdd();
        String[] items = mainServerAdd.split(":");
        if (items.length < 2) {
            throw new MMException("mainServerAdd error:" + mainServerAdd);
        }
        if (!items[0].equalsIgnoreCase("localhost") && !Util.isIP(items[0])) {
            throw new MMException("mainServerAdd error:" + mainServerAdd);
        }
        String host = items[0];
        int port = Integer.parseInt(items[1]);
        int localPort = Server.getEngineConfigure().getNetEventPort();
        if (Util.isLocalHost(host) && port == localPort) {
            logger.info("本服务器被配置为mainServer，但未按照mainServer启动，请重新配置mainServer或按照mainServer启动");
            return;
        }
        NettyServerClient nettyServerClient = new NettyServerClient(ServerType.MAIN_SERVER,1, host, port);
        // 这里必须一直等 直到成功
        nettyServerClient.connectSync(Integer.MAX_VALUE);
        serverClients.put(nettyServerClient.getServerId(), nettyServerClient);
        mainServerClient = nettyServerClient;
        // 告诉mainServer 自己是谁，并且从mainServer哪里获取其它服务器，并连接之
        tellMainServer();
    }


    // 主服务器：别人请求添加，并请求返回其它服务器信息，并告诉其他服务器它的存在
    @NetEventListener(netEvent = SysConstantDefine.TellMainServerSelfInfo)
    public NetEventData registerServerToMain(NetEventData eventData) {
        if (!ServerType.isMainServer()) {
            throw new MMException("this server is not mainServer , serverType=" + ServerType.getServerTypeName());
        }
        // 添加到serverList，返回其它server的List
        ServerInfo serverInfo = (ServerInfo) eventData.getParam();
        ServerInfo old = serverInfos.putIfAbsent(serverInfo.getId(), serverInfo);
        if (old == null) { // 没有添加它
            // 告诉其他服务器，它的存在
            NetEventData serverInfoData = new NetEventData(SysConstantDefine.TellServersNewInfo, eventData.getParam());
            broadcastNetEvent(serverInfoData, false); // 这个地方有可能发送给serverInfo，因为是异步发送的

            // 创建NettyServerClient，并连接
            connectServerSync(serverInfo, Integer.MAX_VALUE, true);

            // 告诉它，所有其他的 ????
            NetEventData ret = new NetEventData(eventData.getNetEvent(), serverInfos);
            return ret;
        }

        throw new MMException("该服务器已经注册完成，是否是断线重连？");
    }

    // 其它服务器：主服务器推出其它服务器的存在：建立与其它服务器的连接
    @NetEventListener(netEvent = SysConstantDefine.TellServersNewInfo)
    public NetEventData receiveServerInfoFromMainServer(NetEventData eventData) {
        ServerInfo serverInfo = (ServerInfo) eventData.getParam();
        if (Util.isLocalHost(serverInfo.getHost()) &&
                serverInfo.getNetEventPort() == Server.getEngineConfigure().getNetEventPort()) { // 过滤掉自己
            return null;
        }
        ServerInfo old = serverInfos.putIfAbsent(serverInfo.getId(), serverInfo);
        if (old == null) {
            connectServerSync(serverInfo, Integer.MAX_VALUE, true);
            return null;
        }
        throw new MMException("该服务器已经注册完成，是否是断线重连？");
    }

    // nettyServerClient断线通知:如果是mainServer，则重连，否则，从client记录去掉它，它连上mainServer自然会重新通知
    @EventListener(event = SysConstantDefine.Event_NettyServerClient_Disconnect)
    public void nettyServerClientDisconnect(EventData eventData) {
        NettyServerClient client = (NettyServerClient) eventData.getData();
        if (mainServerClient == client) { //如果是mainServer,则重连
            // mainServer 应该设置为自动重连
            if (!client.isAutoReconnect()) {
                client.connectSync(3);
            }
            tellMainServer();
        } else { //
            ServerInfo serverInfo = serverInfos.remove(client.getServerId());
            if (serverInfo != null) {
                eventService.fireEventSyn(client.getServerId(), SysConstantDefine.Event_DisconnectNewServer);
            }
            serverClients.remove(client.getServerId());
            if (asyncServerClient == client) {
                asyncServerClient = null;
            }
        }
    }


    private void tellMainServer() {
        // 告诉mainServer 自己是谁，并且从mainServer哪里获取其它服务器，并连接之
        NetEventData netEventData = new NetEventData(SysConstantDefine.TellMainServerSelfInfo);
        netEventData.setParam(selfServerInfo);

        NetEventData ret = fireMainServerNetEventSyn(netEventData); //通知主服务器，并获取其它服务器列表

        Map<Integer, ServerInfo> retServers = (Map) ret.getParam();
        for (Map.Entry<Integer, ServerInfo> entry : retServers.entrySet()) {
            ServerInfo serverInfo = entry.getValue();
            if (entry.getKey() == selfServerInfo.getId()) { // 把自己过滤出来
                continue;
            }
            // 创建NettyServerClient，并连接
            ServerInfo old = serverInfos.putIfAbsent(entry.getKey(), entry.getValue());
            if (old == null) {
                // TODO 这个地方不能同步的，否则一个服务器失败，会影响它的启动
                connectServerSync(serverInfo, Integer.MAX_VALUE, true);
            } else {
                logger.warn("mainServer reStart?");
            }
        }
    }

    public void connectServerSync(ServerInfo serverInfo, int timeout, boolean autoReconect) {
        // 不要自己连自己
        if (serverInfo.getInnerAddress().equals(selfAddress)) {
            return;
        }

        NettyServerClient client = (NettyServerClient) serverClients.get(getServerKey(serverInfo.getHost(), serverInfo.getNetEventPort()));
        if(client != null && client.isConnected()) {
            return;
        }

        client = new NettyServerClient(serverInfo.getType(),serverInfo.getId(), serverInfo.getHost(), serverInfo.getNetEventPort());
        client.setAutoReconnect(autoReconect);
        client.connectSync(timeout);
        if (ServerType.isAsyncServer(serverInfo.getType())) {
            if (asyncServerClient == null) {
                asyncServerClient = client;
            } else {
                throw new MMException("asyncServer 重复");
            }
        }
        addClient(client);
    }

    // 用于自动重连后更新 client
    @EventListener(event = SysConstantDefine.Event_ConnectNewServer)
    public void onClientConnected(EventData eventData) {
        NettyServerClient client = (NettyServerClient) eventData.getData();
        addClient(client);
    }

    private void addClient(NettyServerClient client) {
        serverClients.put(client.getServerId(), client);
        logger.info("client add {} {}", client.getAddress(), serverClients.size());
    }

    // 一个系统的一种NetEvent只有一个监听器(因为很多事件需要返回数据)，可以通过内部事件分发
    public NetEventData handleNetEventData(NetEventData netEventData) {
        NetEventListenerHandler handler = handlerMap.get(netEventData.getNetEvent());
        if (handler == null) {
            throw new MMException("netEventHandle is not exist , netEvent=" + netEventData.getNetEvent());
        }
        // TODO 这里面抛异常如何处理？自己消化，并通知调用服务器异常了，不返回数据的呢？
        NetEventData ret = handler.handle(netEventData);
        return ret;
    }

    /**
     * 事件是异步的
     **/
    public void broadcastNetEvent(final NetEventData netEvent, final boolean self) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // 通过NetEvent的网络接口发出事件
                for (final Map.Entry<Integer, ServerClient> entry : serverClients.entrySet()) {
                    try { // TODO 这里尽量做到不捕获异常，提高效率
                        entry.getValue().push(netEvent); // 这个不等待返回，所以不用多个发送
                    } finally {
                        continue;
                    }
                }
                if (self) {
                    handleNetEventData(netEvent);
                }
            }
        });
    }

    /**
     * 同步触发事假，即事件完成方可返回
     * 别忘了截取一些出问题的事件
     * 显然，这里每个ServerClient并不需要同步等待，
     */
    public Map<Integer, NetEventData> broadcastNetEventSyn(final NetEventData netEvent, boolean self) {
        try {
            final CountDownLatch latch = new CountDownLatch(serverClients.size());
            final Map<Integer, NetEventData> result = new ConcurrentHashMap<>();
            // 通过NetEvent的网络接口发出事件
            for (final Map.Entry<Integer, ServerClient> entry : serverClients.entrySet()) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try { // TODO 这里尽量做到不捕获异常，提高效率
                            NetEventData ret = sendNetEvent(entry.getValue(), netEvent);//(NetEventData)entry.getValue().send(netEvent);
                            if (ret == null) {
                                result.put(entry.getKey(), new NetEventData(netEvent.getNetEvent()));
                            }
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            latch.await();
            if (self) {
                NetEventData ret = handleNetEventData(netEvent);
                result.put(selfServerInfo.getId(), ret);
            }
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("exception happened while fire netEvent :" + netEvent.getNetEvent());
        }
        return null;
    }

    /**
     * 向主服务器发送事件
     * 异步
     */
    public void fireMainServerNetEvent(NetEventData netEvent) {
        if (ServerType.isMainServer()) {
            handleNetEventData(netEvent);
            return;
        }
        if (mainServerClient != null) {
            mainServerClient.push(netEvent);
            return;
        }
        throw new MMException("mainServerClient is null");
    }

    /**
     * 向主服务器发送事件
     */
    public NetEventData fireMainServerNetEventSyn(NetEventData netEvent) {
        if (ServerType.isMainServer()) {
            return handleNetEventData(netEvent);
        }
        if (mainServerClient != null) {
            return sendNetEvent(mainServerClient, netEvent);
        }
        throw new MMException("mainServerClient is null");
    }

    /**
     * 向异步服务器发送事件
     * 异步
     */
    public void fireAsyncServerNetEvent(NetEventData netEvent) {
        if (ServerType.isAsyncServer()) {
            handleNetEventData(netEvent);
            return;
        }
        if (asyncServerClient != null) {
            asyncServerClient.push(netEvent);
            return;
        }
        throw new MMException("asyncServerClient is null");
    }

    /**
     * 向异步服务器发送事件
     */
    public NetEventData fireAsyncServerNetEventSyn(NetEventData netEvent) {
        if (ServerType.isAsyncServer()) {
            return handleNetEventData(netEvent);
        }
        if (asyncServerClient != null) {
            return sendNetEvent(asyncServerClient, netEvent);
        }
        throw new MMException("asyncServerClient is null,");
    }

    /**
     * 向某个服务器发送事件
     * 异步
     */
    public void fireServerNetEvent(String add, NetEventData netEvent) {
        if (add.equals(selfAddress)) {
            handleNetEventData(netEvent);
            return;
        }
        ServerClient serverClient = serverClients.get(add);
        if (serverClient != null) {
            serverClient.push(netEvent);
            return;
        }
        throw new MMException("serverClient is null");
    }

    /**
     * 向某个服务器发送事件
     * 同步
     */
    public NetEventData fireServerNetEventSyn(ServerInfo serverInfo, NetEventData netEvent) {
        String address = serverInfo.getInnerAddress();
        if (address.equals(selfAddress)) {
            return handleNetEventData(netEvent);
        }

        ServerClient serverClient = serverClients.get(address);

        if (serverClient != null && serverClient.isConnected()) {
            return sendNetEvent(serverClient, netEvent);
        }

        // 这里设置一个超时, 不至于中途因网络问题卡死服务器
        connectServerSync(serverInfo, 3, true);

        serverClient = serverClients.get(address);
        if (serverClient != null && serverClient.isConnected()) {
            return sendNetEvent(serverClient, netEvent);
        }

        // 如果是超时, 上面就会抛异常
        throw new MMException("服务器尚未建立连接 " + address);
    }

    public NetEventData sendNetEvent(ServerClient serverClient, NetEventData netEvent) {
        NetEventData ret = (NetEventData) serverClient.request(netEvent);
        if (ret.getNetEvent() == SysConstantDefine.NETEVENTEXCEPTION || ret.getNetEvent() == SysConstantDefine.NETEVENTMMEXCEPTION) {
            throw new MMException((String) ret.getParam());
        } else if (ret.getNetEvent() == SysConstantDefine.NETEVENTTOCLIENTEXCEPTION) {
            JSONObject object = JSONObject.parseObject((String) ret.getParam());
            throw new ToClientException(object.getInteger("errCode"), object.getString("errMsg"));
        }
        return ret;
    }

    public ServerInfo getServerInfo(int serverId){
        return serverInfos.get(serverId);
    }

}
