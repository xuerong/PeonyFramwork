package com.peony.engine.framework.control.netEvent;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.ServerType;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.thread.ThreadPoolHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by apple on 16-8-28.
 * 服务器之间连接时候用的client……
 */
public class NettyServerClient extends AbServerClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerClient.class);
    private static final int timeout = 10;
    private static EventLoopGroup eventLoopGroup;
    private static ScheduledExecutorService connectHolder;

    static {
        int threads = Runtime.getRuntime().availableProcessors();
        eventLoopGroup = new NioEventLoopGroup(threads, new ThreadFactoryBuilder().setNameFormat("EventWorker-%d").build());
        connectHolder = ThreadPoolHelper.newScheduledThreadPoolExecutor("ConnectHolder",3);//
//        new ScheduledThreadPoolExecutor(3, new ThreadFactoryBuilder().setNameFormat("ConnectHolder-%d").build());
    }

    private int serverId;

    private Map<String, NetEventPacket> packetMap = new ConcurrentHashMap<>(); // 正在用的id
    private Channel channel;
    private Bootstrap bootstrap;
    private volatile boolean connected = false;
    private volatile boolean tryConnect = false;
    private boolean autoReconnect = false;
    private long lastConnectMillis = 0;

    private NetEventService netEventService;
    private EventService eventService;

    public NettyServerClient(int serverType,int serverId, String host, int port) {
        this.serverType = serverType;
        this.serverId = serverId;
        this.address = new InetSocketAddress(host, port);

        netEventService = BeanHelper.getServiceBean(NetEventService.class);
        eventService = BeanHelper.getServiceBean(EventService.class);

        bootstrap = new Bootstrap(); // (1)
        bootstrap.group(eventLoopGroup); // (2)
        bootstrap.channel(NioSocketChannel.class); // (3)
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        new DefaultNettyEncoder(),
                        new DefaultNettyDecoder(),
                        new NettyClientHandler()
                );
            }
        });
    }

    @Override
    public void connectSync(int timeout) {
        if (isConnected()) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        Runnable task = new Runnable() {
            public void run() {
                synchronized (address.toString().intern()) {
                    if (isConnected()) {
                        return;
                    }
                    try {
                        tryConnect = true;
                        bootstrap.connect(address).sync();
                        tryConnect = false;
                        latch.countDown();
                        return;
                    } catch (Exception e) {
                        tryConnect = false;
                        logger.error("connect fail. {}", ExceptionUtils.getRootCauseMessage(e));
                        // 不用 while 避免卡死某个线程
                        connectHolder.execute(this);
                    }
                }
            }
        };

        connectHolder.execute(task);

        try {
            latch.await(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            if (!isConnected()) {
                throw new MMException("连接超时 Address " + address, e);
            }
        }

        if (!isConnected()) {
            throw new MMException("连接超时 Address " + address);
        }
    }

    @Override
    public void connectAsync() {
        if (isConnected() || isTryConnect()) {
            return;
        }
        _tryConnect(0);
    }

    public void _tryConnect(int delaySeconds) {
        connectHolder.schedule(() -> {
            synchronized (address.toString().intern()) {
                if (isConnected() || isTryConnect()) {
                    return;
                }
                long currentTimeMillis = System.currentTimeMillis();
                logger.info(String.format("Trying to connect Address %20s %s.", address, lastConnectMillis == 0 ? "first time" : (currentTimeMillis - lastConnectMillis)/1000 + "S from last time"));
                lastConnectMillis = currentTimeMillis;
                tryConnect = true;
                bootstrap.connect(address).addListener((ChannelFutureListener) future -> {
                    tryConnect = false;
                    // 没连上
                    if (!future.isSuccess()) {
                        // 重连
                        _tryConnect(delaySeconds + 5);
                    }
                });
            }
            // 连接间隔增长到60秒就停止增加. 如果网络故障或重启, 最多在网络或服务器恢复60秒之内可以重新连上
        }, Math.min(delaySeconds, 60), TimeUnit.SECONDS);
    }

    class NettyClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            connected = true;
            channel = ctx.channel();
            System.err.println("connect server success " + channel);
            logger.info("connect server {} {}", ServerType.getServerTypeName(serverType), address);

            eventService.fireEventSyn(NettyServerClient.this,SysConstantDefine.Event_ConnectNewServer);

            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            connected = false;
            System.err.println("disconnect server " + channel);
            logger.info("disconnect server {} {}", ServerType.getServerTypeName(serverType), address);

            // 自动重连
            if (isAutoReconnect()) {
                // 给一个延迟
                _tryConnect(5);
            }

            eventService.fireEventSyn(NettyServerClient.this,SysConstantDefine.Event_NettyServerClient_Disconnect);
            eventService.fireEvent(NettyServerClient.this,SysConstantDefine.Event_ConnectNewServerAsy);
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof NetEventPacket) { // 说明是某一个需要返回的值
                connectHolder.execute(() -> {
                    NetEventPacket socketPacket = (NetEventPacket) msg;
                    NetEventPacket s = packetMap.remove(socketPacket.getId());
                    s.setReData(socketPacket.getData());
                    s.getLatch().countDown();
                });
            } else if (msg instanceof NetEventData) {
                NetEventData eventData = (NetEventData) msg;
                if (eventData.getNetEvent() == SysConstantDefine.NETEVENT_PING) {
                    logger.info("ping from server side {}", msg);
                    eventData.setNetEvent(SysConstantDefine.NETEVENT_PONG);
                    ctx.writeAndFlush(eventData);
                } else {
                    logger.info("net event from server side {}", msg);
                }
            } else {
                throw new MMException("NettyServerClient receive data is not SocketPacket,throw away it");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public int getServerId() {
        return serverId;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public boolean isTryConnect() {
        return tryConnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @Override
    public Object request(Object msg) {
        String id = UUID.randomUUID().toString();
        try {
            NetEventPacket packet = new NetEventPacket();
            packet.setId(id);
            packet.setData(msg);
            CountDownLatch latch = new CountDownLatch(1);
            packet.setLatch(latch);
            packetMap.put(id, packet);
            channel.writeAndFlush(packet); // (3)
            if (!latch.await(timeout, TimeUnit.SECONDS)) {
                throw new MMException("timeout while send packet, to " + address);
            }
            return packet.getReData();
        } catch (Throwable e) {
            packetMap.remove(id);
            if (e instanceof InterruptedException) {
                throw new MMException("请求超时:" + e);
            }
            if (!isConnected()) {
                throw new MMException("服务器连接尚未建立 " + address + " msg:" + JSON.toJSONString(msg));
            } else {
                logger.error("request error ", e);
            }
        }
        return null;
    }

    @Override
    public void push(Object msg) {
        try {
            channel.writeAndFlush(msg); // (3)
        } catch (Throwable e) {
            if (!isConnected()) {
                logger.error("服务器连接尚未建立 " + address + " msg:" + JSON.toJSONString(msg));
            } else {
                logger.error("push error msg:" + JSON.toJSONString(msg), e);
            }
        }
    }
}
