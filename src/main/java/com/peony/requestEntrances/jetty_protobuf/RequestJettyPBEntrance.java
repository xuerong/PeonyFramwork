package com.peony.requestEntrances.jetty_protobuf;

import com.peony.engine.framework.control.request.RequestService;
import com.peony.engine.framework.data.entity.account.AccountSysService;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.entity.session.SessionService;
import com.peony.engine.framework.net.tool.HttpHelper;
import com.peony.engine.framework.net.packet.RetPacket;
import com.peony.engine.framework.net.entrance.Entrance;
import com.peony.engine.framework.security.LocalizationMessage;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.util.Util;
import com.peony.requestEntrances.tcp_protobuf.protocol.BasePB;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by a on 2016/9/20.
 */
public class RequestJettyPBEntrance extends Entrance {

    private static final Logger log = LoggerFactory.getLogger(RequestJettyPBEntrance.class);

    private Server server;


    public RequestJettyPBEntrance(){}

    private SessionService sessionService;
    private RequestService requestService;
    private AccountSysService accountSysService;
    @Override
    public void start() throws Exception {
//        log.info(sessionService);
        Handler entranceHandler = new AbstractHandler(){
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
                fire(request,response,"RequestJettyPBEntrance");
            }
        };
        server = new Server(this.port);
        server.setHandler(entranceHandler);
        server.start();
    }

    private void fire(HttpServletRequest request, HttpServletResponse response,String entranceName){
        int opcode=-1;
        RetPacket rePacket = null;
        try {
            byte[] data = HttpHelper.decode(request);
            // 获取controller，并根据controller获取相应的编解码器
            String opcodeStr = request.getHeader(SysConstantDefine.opcodeKey);
            if(StringUtils.isEmpty(opcodeStr) || !StringUtils.isNumeric(opcodeStr)){
                throw new MMException("opcode error,opcode = "+opcodeStr);
            }
            opcode=Integer.parseInt(opcodeStr);
            
            Session session = sessionService.create(request.getRequestURL().toString(), Util.getIp(request));
            session.setLocalization(request.getHeader(SysConstantDefine.localizationKey));

            LocalizationMessage.setThreadLocalization(session.getLocalization());

            rePacket = requestService.handleRequest(opcode, data, session);
            if(rePacket==null){
                // 处理包失败
                throw new MMException("处理消息错误,opcode:"+opcode);
            }
            if(!rePacket.keepSession()){
                sessionService.removeSession(session);
            }
            response.setHeader(SysConstantDefine.opcodeKey,""+rePacket.getOpcode());

            byte[] reData=(byte[])rePacket.getRetData();
            // 这个地方要+1
            response.setBufferSize(reData.length+1);
            response.setContentLength(reData.length);
            response.getOutputStream().write(reData, 0, reData.length);
            response.getOutputStream().flush();
//            response.getOutputStream().close();
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
            }else if(e instanceof IOException){
                log.error("");
                throw new RuntimeException(e);
            }
            BasePB.SCException.Builder scException = BasePB.SCException.newBuilder();
            scException.setCsOpcode(opcode);
            scException.setScOpcode(rePacket != null?rePacket.getOpcode():-1);
            scException.setErrCode(errCode);
            scException.setErrMsg(errMsg);
            byte[] reData = scException.build().toByteArray();

            response.setHeader(SysConstantDefine.opcodeKey,""+SysConstantDefine.Exception);
            response.setBufferSize(reData.length+1);
            response.setContentLength(reData.length);
            try {
                response.getOutputStream().write(reData, 0, reData.length);
                response.getOutputStream().flush();
            }catch (IOException e1){
                e1.printStackTrace();
            }
        }finally {
            LocalizationMessage.removeThreadLocalization();
        }
    }

    @Override
    public void stop() throws Exception {
        if(server != null){
            server.stop();
        }
    }
}
