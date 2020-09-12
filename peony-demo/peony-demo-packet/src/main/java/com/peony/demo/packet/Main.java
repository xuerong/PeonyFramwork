package com.peony.demo.packet;

import com.peony.core.configure.EngineConfigure;
import com.peony.core.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: zhengyuzhen
 * @Date: 2020-08-30 19:59
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args){
        try{
            log.info("start server begin!");
            Server.start();
            log.info("start server end!");
        }catch (Throwable e){
            log.error("start error!",e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
