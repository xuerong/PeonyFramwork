package com.peony.engine.framework.server;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.net.entrance.Entrance;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.configure.EngineConfigure;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.platform.deploy.DeployService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 启动时需把 config 文件夹加到 classpath 中.
 * <p>
 * <p>
 * Created by Administrator on 2015/11/16.
 */
public final class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public static final String Congratulations = "Congratulations";
    public static final String startupSuccess = "startup success";

    private static EngineConfigure configure;
    private static Integer serverId;

    public static Integer getServerId() {
        return serverId;
    }

    /**
     * 初始化配置文件
     * 初始化所有helper
     * 启动游戏主循环
     **/
    public static void init(EngineConfigure configure) {
        Server.configure = configure;
        Server.serverId = configure.getInteger("serverId");

        if(configure.getInteger("serverId") == null) {
            log.error("mmserver.properties 缺少配置[服务器 id] serverId ");
            throw new MMException(MMException.ExceptionType.StartUpFail,"mmserver.properties 缺少配置[服务器 id] serverId");
        }
    }

    public static void init(String serverTypeStr) {
        init(new EngineConfigure(serverTypeStr));
    }

    public static void init() {
        init(new EngineConfigure());
    }

    public static void start() {
        log.info("服务器启动开始!");

        URL resource = Server.class.getClassLoader().getResource("./config/csv");
        if(resource == null){
            resource = Server.class.getClassLoader().getResource("./csv");
        }
        if(resource == null) {
            log.error("请把config所在的路径[.]加到 classpath 中.");
            throw new MMException(MMException.ExceptionType.StartUpFail,"请把config所在的路径[.]加到 classpath 中.");
        } else {
            resource = Server.class.getClassLoader().getResource("log4j.properties");
            if(resource == null) {
                log.error("请把config目录[./config]加到 classpath 中.");
                throw new MMException(MMException.ExceptionType.StartUpFail,"请把config目录[./config]加到 classpath 中.");
            }
        }

        // Service的初始化
        Map<Class<?>, Object> serviceBeanMap = BeanHelper.getServiceBeans();
        Map<Integer, Map<Class<?>, Method>> initMethodMap = ServiceHelper.getInitMethodMap();

        for (Map.Entry<Integer, Map<Class<?>, Method>> entry : initMethodMap.entrySet()) {
            for (Map.Entry<Class<?>, Method> methodEntry : entry.getValue().entrySet()) {
                Object object = serviceBeanMap.get(methodEntry.getKey());
                if (object == null) {
                    throw new MMException("find not service object , service class = " + methodEntry.getKey());
                }
                try {
                    methodEntry.getValue().invoke(object);
                    log.info("init service {} finish", methodEntry.getKey());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("init service " + methodEntry.getKey() + " fail ", e);
                } finally { // 报异常，这里是停服务器还是继续？
                    continue;
                }
            }
        }

        // 启动所有入口
//        List<Entrance> entranceList = configure.getEntranceList();
        Collection<Entrance> entranceList = BeanHelper.getEntranceBeans().values();

        for (Entrance entrance : entranceList) {
            try {
                entrance.start();
            } catch (Exception e) {
//                e.printStackTrace();
                log.error("net start fail , net name = " + entrance.getName() + "," + e.getMessage());
                try {
                    entrance.stop();
                } catch (Exception e2) {
                    log.error("net stop fail , net name = " + entrance.getName() + ":" + e2.getStackTrace());
                }
            }
        }

        // 等待启动条件完成
        MonitorService monitorService = BeanHelper.getServiceBean(MonitorService.class);
        monitorService.startWait();
        // 服务器启动完成
        EventService eventService = BeanHelper.getServiceBean(EventService.class);
        eventService.fireEventSyn(null,SysConstantDefine.Event_ServerStart);
        eventService.fireEvent(null,SysConstantDefine.Event_ServerStartAsync);
        // 关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("服务器关闭开始");
                Server.stop();
            }
        });

        printStartMsg();
    }

    public static void printStartMsg(){
        int length = 60;
        String str = String.format("server(ID:%3d) "+startupSuccess+"!", getServerId());
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb3 = new StringBuilder();
        for(int i=0;i<length+1;i++){
            sb1.append("-");
        }
        for(int i=0;i<(length-Congratulations.length())/2;i++){
            sb2.append(" ");
        }
        for(int i=0;i<(length-str.length())/2;i++){
            sb3.append(" ");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("\n"+sb1.toString()+"\n");
        builder.append("|"+sb2.toString()+Congratulations+sb2.toString()+"|\n");
        builder.append("|"+sb3.toString()+str+sb3.toString()+"|\n");
        builder.append(""+sb1.toString()+"\n");
        log.info(builder.toString());
    }

    public static void stop() {
        // 关闭入口
        Collection<Entrance> entranceList = BeanHelper.getEntranceBeans().values();
        for (Entrance entrance : entranceList) {
            try {
                entrance.stop();
            } catch (Exception e) {
                log.error("net stop fail , net name = " + entrance.getName() + ":" + e.getStackTrace());
            }
        }
        // 关闭所有的Service
        Map<Class<?>, Object> serviceBeanMap = BeanHelper.getServiceBeans();
        Map<Integer, Map<Class<?>, Method>> destroyMethodMap = ServiceHelper.getDestroyMethodMap();
        for (Map.Entry<Integer, Map<Class<?>, Method>> f : destroyMethodMap.entrySet()) {
            for (Map.Entry<Class<?>, Method> entry : f.getValue().entrySet()) {
                Object object = serviceBeanMap.get(entry.getKey());
                if (object == null) {
                    throw new MMException("find not service object , service class = " + entry.getKey());
                }
                long start = System.currentTimeMillis();
                Method method = entry.getValue();
                if (method != null) {
                    try {
                        method.invoke(object);
                        log.info("destroy service {} finish", entry.getKey());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        log.error("destroy service " + entry.getKey() + " fail ", e);
                    } catch (Throwable e) {
                        log.error("destroy service " + entry.getKey() + " fail ", e);
                    } finally { // 报异常，这里是停服务器还是继续？
                        log.info("stop {} finish use {}", entry.getKey().getSimpleName(), System.currentTimeMillis() - start);
                    }
                }
            }
        }

        for(Entrance entrance:BeanHelper.getEntranceBeans().values()) {
            long start = System.currentTimeMillis();
            try {
                entrance.stop();
            } catch (Throwable e) {
                log.error("stop entrance " + entrance.getName() + " error", e);
            } finally { // 报异常，这里是停服务器还是继续？
                log.info("stop entrance {} finish use {}", entrance.getName(), System.currentTimeMillis() - start);
            }
        }

        // 等待关闭条件完成
        MonitorService monitorService = BeanHelper.getServiceBean(MonitorService.class);
        monitorService.stopWait();

        log.info("服务器关闭完成!");
    }

    public static EngineConfigure getEngineConfigure() {
        if (configure == null) {
            log.error("configure is not init,don't getEngineConfigure() before server start");
            throw new RuntimeException("configure is not init,don't getEngineConfigure() before server start");
        }
        return configure;
    }

    /**
     * 应用程序启动，如果在容器中运行，请在容器中调用init和start方法
     * <p>
     * 注意: 启动时需把 config 文件夹加到 classpath 中.
     *
     *          :./config/:.
     *
     * @param args
     */
    public static void main(String[] args) {
        try{
            EngineConfigure configure = new EngineConfigure();
            Server.init(configure);
            Server.start();
//            log.info(DeployService.started);
        }catch (Throwable e){
            log.error("start error!");
            log.error(DeployService.startError);
            System.exit(1);
        }

    }
}
