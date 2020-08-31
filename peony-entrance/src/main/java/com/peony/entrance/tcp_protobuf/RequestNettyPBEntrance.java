package com.peony.entrance.tcp_protobuf;

import com.peony.core.control.BeanHelper;
import com.peony.core.control.request.RequestService;
import com.peony.core.data.entity.account.AccountSysService;
import com.peony.core.data.entity.session.Session;
import com.peony.core.data.entity.session.SessionService;
import com.peony.core.net.packet.RetPacket;
import com.peony.core.net.entrance.Entrance;
import com.peony.core.net.tool.NettyHelper;
import com.peony.core.security.LocalizationMessage;
import com.peony.common.exception.MMException;
import com.peony.common.exception.ToClientException;
import com.peony.core.server.SysConstantDefine;
import com.peony.core.net.packet.NettyPBPacket;
import com.peony.entrance.tcp_protobuf.protocol.BasePB;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by a on 2016/9/19.
 */
public class RequestNettyPBEntrance extends Entrance {
    private static final Logger log = LoggerFactory.getLogger(RequestNettyPBEntrance.class);

    Channel channel = null;
    private static AccountSysService accountSysService;
    private static SessionService sessionService;
    private static RequestService requestService;
    private static AccountService accountService;
    @Override
    public void start() throws Exception {
        accountSysService = BeanHelper.getServiceBean(AccountSysService.class);
        sessionService = BeanHelper.getServiceBean(SessionService.class);
        requestService = BeanHelper.getServiceBean(RequestService.class);
        accountService = BeanHelper.getServiceBean(AccountService.class);

        channel = NettyHelper.createAndStart(
                port,RequestNettyPBEncoder.class,RequestNettyPBDecoder.class,new RequestNettyPBHandler(),name);
        log.info("RequestNettyPBEntrance bind port :"+port);
    }

    @Override
    public void stop() throws Exception {

    }

    public static class RequestNettyPBHandler extends SimpleChannelInboundHandler {
        static AttributeKey<String> sessionKey = AttributeKey.newInstance("sessionKey");
        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception { // (1)
            super.channelActive(ctx);
            log.info("connect "+ctx.channel().remoteAddress().toString());
        }
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            String sessionId = ctx.channel().attr(sessionKey).get();
            if(sessionId != null) {
                Session session = accountSysService.netDisconnect(sessionId);
                log.info("disConnect,ip = {},sessionId = {},userId = {}",ctx.channel().remoteAddress().toString(),sessionId,session==null?null:session.getUid());
            }else{
                log.error("disConnect , but session = {},ip = {}",sessionId,ctx.channel().remoteAddress().toString());
            }
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Object msg) { // (2)
            NettyPBPacket nettyPBPacket = (NettyPBPacket) msg;
//            log.info("nettyPBPacket.getOpcode() = "+nettyPBPacket.getOpcode());
            RetPacket retPacket = null;
            try {
                String sessionId = ctx.channel().attr(sessionKey).get();
                if (nettyPBPacket.getOpcode() == SysConstantDefine.LoginOpcode) { // 登陆消息
                    retPacket = accountService.login(nettyPBPacket.getOpcode(),nettyPBPacket.getData(),ctx,sessionKey);
                }else{
                    Session session = checkAndGetSession(sessionId);
                    LocalizationMessage.setThreadLocalization(session.getLocalization());
                    retPacket = requestService.handleRequest(nettyPBPacket.getOpcode(),nettyPBPacket.getData(),session);
                }
                if(retPacket == null){
                    throw new MMException("server error!");
                }
                nettyPBPacket.setOpcode(retPacket.getOpcode());
                nettyPBPacket.setData((byte[])retPacket.getRetData());
                ctx.writeAndFlush(nettyPBPacket);
            }catch (Throwable e){
                int errCode = -1000;
                String errMsg = "系统异常";
                if(e instanceof MMException){
                    MMException mmException = (MMException)e;
                    log.error("MMException:"+mmException.getMessage());
                }else if(e instanceof ToClientException){
                    ToClientException toClientException = (ToClientException)e;
                    errCode = toClientException.getErrCode();
                    errMsg = toClientException.getMessage();
                    log.error("ToClientException:"+toClientException.getMessage());
                }else {
                    log.error("",e);
                }
                BasePB.SCException.Builder scException = BasePB.SCException.newBuilder();
                scException.setCsOpcode(nettyPBPacket.getOpcode());
                scException.setScOpcode(retPacket!=null?retPacket.getOpcode():-1);
                scException.setErrCode(errCode);
                scException.setErrMsg(errMsg);
                nettyPBPacket.setOpcode(SysConstantDefine.Exception);
                nettyPBPacket.setData(scException.build().toByteArray());
                ctx.writeAndFlush(nettyPBPacket);
            }finally {
                LocalizationMessage.removeThreadLocalization();
            }
        }
        private Session checkAndGetSession(String sessionId){
            if (sessionId == null || sessionId.length() == 0){
                throw new MMException("won't get sessionId while :"+sessionId);
            }
            // 不是login，可以处理消息
            Session session = sessionService.get(sessionId);
            if(session == null){
                throw new MMException("login timeout , please login again");
            }
            return session;
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            log.warn(cause.getMessage());
//            cause.printStackTrace();
            ctx.close();
        }
    }
}
