package com.peony.engine.framework.control.netEvent;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.cluster.ServerInfo;
import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.NetEventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.ServerType;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.thread.ThreadPoolHelper;
import com.peony.engine.framework.tool.util.Util;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * TODO remove方法
 */
@Service(init = "init",initPriority = 1)
public class NetEventService {
    private static final Logger logger = LoggerFactory.getLogger(NetEventService.class);

    private static int processors = Runtime.getRuntime().availableProcessors();

    private Map<Integer, NetEventListenerHandler> handlerMap = null;
    // 最多平均每个线程有10个请求等待处理
    private final ThreadPoolExecutor executor = ThreadPoolHelper.newThreadPoolExecutor("PpeonyNetEvent",64,1024,65536);

    // 所有的serverClient 不包括自己 TODO 一个server可能既是这个server又是那个server
    private Map<Integer, ServerClient> serverClients = new ConcurrentHashMap<>();

    // mainServer 不包括自己
    private ServerClient asyncServerClient;

    private String selfAddress;

    public void init() {
        handlerMap = new HashMap<>();
        TIntObjectHashMap<Class<?>> netEventHandlerClassMap = ServiceHelper.getNetEventListenerHandlerClassMap();
        netEventHandlerClassMap.forEachEntry((i, aClass) -> {
            handlerMap.put(i, (NetEventListenerHandler) BeanHelper.getServiceBean(aClass));
            return true;
        });

        selfAddress = getServerKey(Util.getHostAddress(), Server.getEngineConfigure().getNetEventPort());
        System.err.println("selfAddress " + selfAddress);

    }

    private String getServerKey(String host, int port) {
        return host + ":" + port;
    }

    @NetEventListener(netEvent = SysConstantDefine.NETEVENT_PONG)
    public NetEventData pong(NetEventData eventData) {
        logger.info("receive poing from {}", eventData.getChannel());
        return null;
    }

    // 用于自动重连后更新 client
    @EventListener(event = SysConstantDefine.Event_ConnectNewServer)
    public void onClientConnected(NettyServerClient client) {
        addClient(client);
    }


    /**
     * 同步注册服务器，连接上该服务器之后，才返回
     * 如果是自己，直接返回
     * @param id 服务器id
     * @param host 服务器主机
     * @param port 服务器端口
     * @return 服务器对象，用于通信，状态判断等
     */
    public ServerClient registerServerSyn(int id,String host,int port) {
        return registerServerSyn(id, host, port,Integer.MAX_VALUE,true);
    }
    public ServerClient registerServerSyn(int id,String host,int port, int timeout, boolean autoReconect) {
        if(Server.getServerId() == id){
            return null;
        }
        // 不要自己连自己
        if (host.equals(Util.getHostAddress()) && port == Server.getEngineConfigure().getNetEventPort()) {
            throw new MMException("server address error!");
        }

        NettyServerClient client = (NettyServerClient) serverClients.get(getServerKey(host, port));
        if(client != null && client.isConnected()) {
            return client;
        }

        client = new NettyServerClient(ServerType.NODE_SERVER,id, host, port);
        client.setAutoReconnect(autoReconect);
        client.connectSync(timeout);
        addClient(client);
        return client;
    }

    public ServerClient registerServerAsync(int id,String host,int port) {
        if(Server.getServerId() == id){
            return null;
        }
        // 不要自己连自己
        if (host.equals(Util.getHostAddress()) && port == Server.getEngineConfigure().getNetEventPort()) {
            throw new MMException("server address error!");
        }

        NettyServerClient client = (NettyServerClient) serverClients.get(getServerKey(host, port));
        if(client != null && client.isConnected()) {
            return client;
        }

        client = new NettyServerClient(ServerType.NODE_SERVER,id, host, port);
        client.setAutoReconnect(true);
        client.connectAsync();
        addClient(client);
        return client;
    }

    private void addClient(NettyServerClient client) {
        serverClients.put(client.getServerId(), client);
        logger.info("client add {} {} {}", client.getServerId(),client.getAddress(), serverClients.size());
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
                result.put(Server.getServerId(), ret);
            }
            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            logger.error("exception happened while fire netEvent :" + netEvent.getNetEvent());
        }
        return null;
    }

//    /**
//     * 向主服务器发送事件
//     * 异步
//     */
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

//    /**
//     * 向主服务器发送事件
//     */
//    public NetEventData fireMainServerNetEventSyn(NetEventData netEvent) {
//        if (ServerType.isMainServer()) {
//            return handleNetEventData(netEvent);
//        }
//        if (mainServerClient != null) {
//            return sendNetEvent(mainServerClient, netEvent);
//        }
//        throw new MMException("mainServerClient is null");
//    }

//    /**
//     * 向异步服务器发送事件
//     * 异步
//     */
//    public void fireAsyncServerNetEvent(NetEventData netEvent) {
//        if (ServerType.isAsyncServer()) {
//            handleNetEventData(netEvent);
//            return;
//        }
//        if (asyncServerClient != null) {
//            asyncServerClient.push(netEvent);
//            return;
//        }
//        throw new MMException("asyncServerClient is null");
//    }

//    /**
//     * 向异步服务器发送事件
//     */
//    public NetEventData fireAsyncServerNetEventSyn(NetEventData netEvent) {
//        if (ServerType.isAsyncServer()) {
//            return handleNetEventData(netEvent);
//        }
//        if (asyncServerClient != null) {
//            return sendNetEvent(asyncServerClient, netEvent);
//        }
//        throw new MMException("asyncServerClient is null,");
//    }

    /**
     * 向某个服务器发送事件
     * 异步
     */
    public void fireServerNetEvent(int id, NetEventData netEvent) {
        if (Server.getServerId() == id) {
            handleNetEventData(netEvent);
            return;
        }
        ServerClient serverClient = serverClients.get(id);
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
    public NetEventData fireServerNetEventSyn(int id, NetEventData netEvent) {
        if (Server.getServerId() == id) {
            return handleNetEventData(netEvent);
        }

        ServerClient serverClient = serverClients.get(id);

        if (serverClient != null && serverClient.isConnected()) {
            return sendNetEvent(serverClient, netEvent);
        }
        // 如果是超时, 上面就会抛异常
        throw new MMException(MMException.ExceptionType.RemoteFail,"服务器尚未建立连接 ,address={}," ,serverClient==null?null:serverClient.getAddress());
    }

    public NetEventData sendNetEvent(ServerClient serverClient, NetEventData netEvent) {
        try {
            NetEventData ret = (NetEventData) serverClient.request(netEvent);
            if(ret == null){
                throw new MMException(MMException.ExceptionType.SendNetEventFail,"serverClient is null");
            }

            if (ret.getNetEvent() == SysConstantDefine.NETEVENTEXCEPTION || ret.getNetEvent() == SysConstantDefine.NETEVENTMMEXCEPTION) {
                throw new MMException((String) ret.getParam());
            } else if (ret.getNetEvent() == SysConstantDefine.NETEVENTTOCLIENTEXCEPTION) {
                JSONObject object = JSONObject.parseObject((String) ret.getParam());
                throw new ToClientException(object.getInteger("errCode"), object.getString("errMsg"));
            }
            return ret;
        }catch (Throwable e){
            logger.error("sendNetEvent error!");
            throw new MMException(MMException.ExceptionType.SendNetEventFail,"serverClient is null");
        }
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
        // 如果是超时, 上面就会抛异常
        throw new MMException(MMException.ExceptionType.RemoteFail,"服务器尚未建立连接 " + address);
    }



}
