package com.peony.engine.framework.cluster;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.net.entrance.Entrance;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.SysConstantDefine;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 主服的用户入口，用于获取自己在哪个服务器
 *
 * @author zhenguyzhen
 */
public class MainServerClientEntrance extends Entrance {
    private static final Logger logger = LoggerFactory.getLogger(MainServerClientEntrance.class);

    private MainServerService mainServerService;

    private Server server;

    public MainServerClientEntrance() {
    }

    @Override
    public void start() throws Exception {
        try {
            server = new Server(port);
            String resourceBase = "www/" + name;

            server.setHandler(new MainServerClientHandler());
            server.start();
            logger.info(name + " HTTP启动, 使用url: http://localhost:" + port + "/" + name + " 进行访问");
        } catch (Throwable e) {
            throw new MMException("Entrance start error " + name, e);
        }
    }

    public class MainServerClientHandler extends AbstractHandler{

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String deviceId = request.getParameter("deviceId");
            if(StringUtils.isEmpty(deviceId)){

                JSONObject ret = new JSONObject();
                ret.put("code", SysConstantDefine.Exception);
                response.getWriter().write(ret.toJSONString());
                response.getWriter().flush();
                logger.info("deviceId is empty ,response = {}",ret);

                return;
            }
            ServerInfo serverInfo = mainServerService.getServerInfoByDeviceId(deviceId);
            if(serverInfo == null){

                JSONObject ret = new JSONObject();
                ret.put("code", SysConstantDefine.Exception);
                response.getWriter().write(ret.toJSONString());
                response.getWriter().flush();
                logger.error("server info is null,deviceId={},response={}",deviceId,ret);
                return;
            }
            JSONObject ret = new JSONObject();
            ret.put("code", 200);
            ret.put("serverInfo",serverInfo.toClientJson());
            response.getWriter().write(ret.toJSONString());
            response.getWriter().flush();
            logger.info("request for main server,deviceId={},response={}",deviceId,ret);
        }
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
