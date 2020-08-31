package com.peony.core.control.netEvent;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.net.entrance.Entrance;

import com.peony.core.net.tool.NettyHelper;
import com.peony.common.exception.MMException;
import com.peony.common.exception.ToClientException;
import com.peony.core.server.SysConstantDefine;
import com.peony.core.control.BeanHelper;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by a on 2016/8/29.
 */
public class NetEventNettyEntrance extends Entrance {
    private static final Logger log = LoggerFactory.getLogger(NetEventNettyEntrance.class);
    static NetEventService netEventService;
    private Channel serverChannel;
    private Map<String, Channel> clients = new ConcurrentHashMap<>();

    @Override
    public void start() throws Exception {
        serverChannel = NettyHelper.createAndStart(
                port, DefaultNettyEncoder.class, DefaultNettyDecoder.class, new NetEventHandler(),
                new IdleStateHandler(300, 300, 300),
                name);
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
        if (serverChannel != null && serverChannel.isActive()) {
            log.info("NetEventNettyEntrance bind port :" + port);
        } else {
            throw new MMException("NetEventNettyEntrance start fail");
        }
    }

    @ChannelHandler.Sharable
    public class NetEventHandler extends SimpleChannelInboundHandler {
        NetEventData pingEvent = new NetEventData(SysConstantDefine.NETEVENT_PING);

        public NetEventHandler() {

        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            Channel channel = ctx.channel();
            clients.put(channel.id().asLongText(), channel);
            log.info("client active {} total clients {}", channel, clients.size());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            Channel channel = ctx.channel();
            clients.remove(channel.id().asLongText());
            log.info("client inActive {} total clients {}", channel, clients.size());
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleState state = ((IdleStateEvent) evt).state();
                if (state == IdleState.ALL_IDLE) {
                    ctx.writeAndFlush(pingEvent);
                    log.info("send ping to {}", ctx.channel());
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("exception caught {} {}", cause.getMessage(), ctx.channel());
            ctx.close();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            NetEventData netEventData = null;
            String id = null;
            if (msg instanceof NetEventPacket) {
                NetEventPacket socketPacket = (NetEventPacket) msg;
                if (socketPacket.getData() instanceof NetEventData) {
                    netEventData = (NetEventData) socketPacket.getData();
                }
                id = socketPacket.getId();
            } else if (msg instanceof NetEventData) {
                netEventData = (NetEventData) msg;
            }
            try {
                if (netEventData == null) {
                    throw new MMException("NetEventNettyEntrance 收到包错误 ：" + msg.getClass().getName());
                }
                netEventData.setChannel(ctx.channel());
                NetEventData retPacket = netEventService.handleNetEventData(netEventData);
                if (id != null) { // 需要返回的
                    NetEventPacket socketPacket = new NetEventPacket();
                    socketPacket.setId(id);
                    socketPacket.setData(retPacket);
                    ctx.writeAndFlush(socketPacket);
                }
            } catch (Throwable e) {
                if (id != null) {
                    NetEventPacket netEventPacket = new NetEventPacket();
                    netEventPacket.setId(id);
                    NetEventData eventData = new NetEventData(SysConstantDefine.NETEVENTEXCEPTION);
                    JSONObject data = new JSONObject();
                    if (e instanceof MMException) {
                        eventData.setNetEvent(SysConstantDefine.NETEVENTMMEXCEPTION);
                        MMException mmException = (MMException) e;
                        data.put("errCode", mmException.getExceptionType());
                        data.put("errMsg", mmException.getMessage());
                    } else if (e instanceof ToClientException) {
                        eventData.setNetEvent(SysConstantDefine.NETEVENTTOCLIENTEXCEPTION);
                        ToClientException exception = (ToClientException) e;
                        data.put("errCode", exception.getErrCode());
                        data.put("errMsg", exception.getMessage());
                    } else {
                        data.put("errMsg", e.getMessage());
                    }
                    eventData.setParam(data.toJSONString());
                    netEventPacket.setData(eventData);
                    ctx.writeAndFlush(netEventPacket);
                }
            }
        }
    }

    @Override
    public void stop() throws Exception {
        serverChannel.close();
    }
}
