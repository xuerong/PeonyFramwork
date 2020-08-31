package com.peony.core.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemLog {
    private static final Logger log = LoggerFactory.getLogger(SystemLog.class);
    public static void log(String msg,Object... params){
        log.info(msg,params);
    }
    public static void log(Object msg,Object... params){
        log.info(msg==null?"null":msg.toString(),params);
    }
}
