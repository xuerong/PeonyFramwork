package com.peony.core.net.entrance.http;

import com.peony.core.net.entrance.Entrance;
import com.peony.common.exception.MMException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用的 Http 入口
 *
 * @author jiangmin.wu
 */
public class JettyEntrance extends Entrance {
    private static final Logger logger = LoggerFactory.getLogger(JettyEntrance.class);

    private Server server;

    public JettyEntrance() {
    }

    @Override
    public void start() throws Exception {
        try {
            server = new Server(port);
            String resourceBase = "www/" + name;
            WebAppContext context = new WebAppContext();
            context.setDescriptor(resourceBase + "/WEB-INF/web.xml");
            context.setResourceBase(resourceBase);
            context.setContextPath("/"+name);
            context.setParentLoaderPriority(true);
            context.setClassLoader(Server.class.getClassLoader());
            server.setHandler(context);
            server.start();
            logger.info(name + " HTTP启动, 使用url: http://localhost:" + port + "/" + name + " 进行访问");
        } catch (Throwable e) {
            throw new MMException("Entrance start error " + name, e);
        }
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
