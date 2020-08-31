package com.peony.platform.deploy.util;

import com.jcraft.jsch.SftpProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
  * 注释
  *
  * @Author： coding99
  * @Date： 16-9-2
  * @Time： 下午8:36
  */


public class MyProgressMonitor implements SftpProgressMonitor {

    private static final Logger logger = LoggerFactory.getLogger(MyProgressMonitor.class);


    private long transfered;


    @Override
    public boolean count(long count) {
        transfered = transfered + count;
        logger.debug("Currently transferred total size: " + transfered + " bytes");
        return true;
    }


    @Override
    public void end() {
        logger.debug("Transferring done.");
    }


    @Override
    public void init(int op, String src, String dest, long max) {
        logger.debug("Transferring begin.");
    }

}
