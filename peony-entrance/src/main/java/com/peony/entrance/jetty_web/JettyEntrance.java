package com.peony.entrance.jetty_web;

import com.peony.core.net.entrance.Entrance;
import com.peony.common.exception.MMException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

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

            // 获取jar包里面的webapp路径
            final URL webappUrl = JettyEntrance.class.getClassLoader().getResource(resourceBase);
            final String webappUrlStr = webappUrl.toExternalForm();
            System.out.println(webappUrlStr);
            context.setContextPath("/"+name);
            // 关键步骤 : 设置webapp的目录路径:  jar:file:/path_to_jar!/webapp
            context.setWar(webappUrlStr);
            context.setDescriptor(webappUrlStr + "/WEB-INF/web.xml");

//            context.setDescriptor(resourceBase + "/WEB-INF/web.xml");
            context.setResourceBase(webappUrlStr);
//            context.setContextPath("/"+name);
            context.setParentLoaderPriority(true);
            context.setClassLoader(Thread.currentThread().getContextClassLoader());
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
