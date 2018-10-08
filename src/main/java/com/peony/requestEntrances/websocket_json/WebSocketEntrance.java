package com.peony.requestEntrances.websocket_json;

import com.peony.engine.framework.control.request.RequestService;
import com.peony.engine.framework.data.entity.account.AccountSysService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.entity.session.SessionService;
import com.peony.engine.framework.net.entrance.Entrance;
import com.peony.engine.framework.net.tool.NettyHelper;
import com.peony.engine.framework.security.LocalizationMessage;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.*;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by a on 2016/9/19.
 */
public class WebSocketEntrance extends Entrance {
    private static final Logger log = LoggerFactory.getLogger(WebSocketEntrance.class);

    Channel channel = null;
    private static AccountSysService accountSysService;
    private static SessionService sessionService;
    private static RequestService requestService;
    private static AccountService accountService;
    @Override
    public void start() throws Exception {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        channel = NettyHelper.createAndStartWebSocket(port,name,WebSocketHandler.class,WebSocketOutboundChannelHandler.class);
        log.info("WebSocketEntrance bind port :"+port);
    }

    @Override
    public void stop() throws Exception {
        // 停止网络
        channel.close();
    }

    public static class WebSocketHandler extends SimpleChannelInboundHandler {
        static AttributeKey<String> sessionKey = AttributeKey.newInstance("sessionKey");
        static AttributeKey<String> ipKey = AttributeKey.newInstance("ipKey");

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception { // (1)
            super.channelActive(ctx);
            log.info("server get connect "+ctx.channel().remoteAddress().toString());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            String sessionId = ctx.channel().attr(sessionKey).get();
            if(sessionId != null) {
                Session session = accountSysService.netDisconnect(sessionId);
                log.info("disConnect,ip = {},sessionId = {},userId = {}",ctx.channel().remoteAddress().toString(),sessionId,session==null?null:session.getAccountId());
            }else{
                log.error("disConnect , but session = {},ip = {}",sessionId,ctx.channel().remoteAddress().toString());
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
//            System.out.println("sss"+msg);
            if (msg instanceof FullHttpRequest) {

                FullHttpRequest req = (FullHttpRequest)msg;
                // 如果HTTP解码失败，返回HHTP异常
                if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
                    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,BAD_REQUEST));
                    return;
                }
                // 构造握手响应返回，本机测试
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                        "ws://localhost:port/websocket", null, false);
                WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory
                            .sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    handshaker.handshake(ctx.channel(), req);
                    // 握手的时候记录IP地址
                    HttpHeaders headers = req.headers();
                    String ip = headers.get("X-Real-IP");
                    if(ip == null){
                        ip = headers.get("X-Forwarded-For");
                    }
                    ctx.channel().attr(ipKey).set(ip);
                }
            } else if (msg instanceof WebSocketFrame) {
                WebSocketFrame frame = (WebSocketFrame)msg;
                // 判断是否是关闭链路的指令
                if (frame instanceof CloseWebSocketFrame) {
                    ctx.close();
                    return;
                }
                // 判断是否是Ping消息
                if (frame instanceof PingWebSocketFrame) {
                    frame.content().retain();
                    ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
                    return;
                }
                // 本例程仅支持文本消息，不支持二进制消息
                if (!(frame instanceof TextWebSocketFrame)) {
                    throw new UnsupportedOperationException(String.format(
                            "%s frame types not supported", frame.getClass().getName()));
                }

                JSONObject reqJson = null;
                int opcode = -1;
                try {
                    // 返回应答消息
                    String request = ((TextWebSocketFrame) frame).text();
                    reqJson = JSONObject.parseObject(request);
                    opcode = reqJson.getInteger("id");
                    JSONObject retPacket = null;
                    String sessionId = ctx.channel().attr(sessionKey).get();
                    if (opcode == SysConstantDefine.LoginOpcode) { // 登陆消息
                        JSONObject data = reqJson.getJSONObject("data");
                        data.put("ip",ctx.channel().attr(ipKey).get());
                        retPacket = accountService.login(opcode,data,ctx,sessionKey);
                        sessionId = ctx.channel().attr(sessionKey).get();
                        log.info("session:{} login, req msg:{},response msg:{}", sessionId, reqJson.toString(), retPacket.toString());
                    }else{
                        Session session = checkAndGetSession(sessionId);
                        if(opcode != SysConstantDefine.Heart) {
                            log.info("user:{},request msg:(msgid: {}[{}]),{}", session.getAccountId(), requestService.getOpName(opcode),opcode, reqJson.toString());
                        }
                        LocalizationMessage.setThreadLocalization(session.getLocalization());
                        retPacket = requestService.handleRequest(opcode,reqJson.getJSONObject("data"),session);
                        if(opcode != SysConstantDefine.Heart) {
                            log.info("user:{},response msg:(msgid: {}[{}]),{}", session.getAccountId(), requestService.getOpName(opcode),opcode, retPacket.toString());
                        }
                    }

                    if(retPacket == null){
                        throw new MMException("server error!");
                    }
                    JSONObject ret = new JSONObject();
                    ret.put("id",opcode);
                    ret.put("data",retPacket);
                    if(reqJson.containsKey("seq")){
                        ret.put("seq", reqJson.get("seq"));
                    }
                    ctx.writeAndFlush(new TextWebSocketFrame(ret.toString()));
                }catch (Throwable e){

                    int errCode = -1000;
                    String errMsg = "handler client msg exception!";
                    if(e instanceof MMException){
                        MMException mmException = (MMException)e;
                        log.error("handler msg exception:", e);
                    }else if(e instanceof ToClientException){
                        ToClientException toClientException = (ToClientException)e;
                        errCode = toClientException.getErrCode();
                        errMsg = toClientException.getMessage();
                        log.error("catch to client exception errCode:{}[{}], errMsg:{}", requestService.getExceptionName(errCode),errCode, errMsg);
                    }else{
                        log.error("handler msg exception:", e);
                    }

                    JSONObject ret = new JSONObject();
                    ret.put("id", SysConstantDefine.Exception);
                    JSONObject data = new JSONObject();
                    data.put("cmd", opcode);
                    data.put("errorCode",errCode);
                    data.put("errorMsg",errMsg);
                    ret.put("data",data);
                    if(reqJson != null && reqJson.containsKey("seq")) {
                        ret.put("seq", reqJson.get("seq"));
                    }
                    ctx.writeAndFlush(new TextWebSocketFrame(ret.toString()));
                } finally {
                    LocalizationMessage.removeThreadLocalization();
                }
            } else {
                throw new RuntimeException("无法处理的请求");
            }
        }

        private void sendHttpResponse(ChannelHandlerContext ctx,
                                             FullHttpRequest req, FullHttpResponse res) {
            // 返回应答给客户端
            if (res.status().code() != 200) {
                ByteBuf buf = ByteBufUtil.writeUtf8(ctx.alloc(), res.status().toString());
                try {
                    res.content().writeBytes(buf);
                } finally {
                    buf.release();
                }
                HttpUtil.setContentLength(res, res.content().readableBytes());
            }

            // 如果是非Keep-Alive，关闭连接
            ChannelFuture f = ctx.channel().writeAndFlush(res);
            if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
                f.addListener(ChannelFutureListener.CLOSE);
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
