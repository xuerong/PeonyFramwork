package com.peony.demo.proxyTest;

import com.peony.common.tool.helper.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoTest {
    private static final Logger log = LoggerFactory.getLogger(DoTest.class);
    /**
     * 测试aop功能
     *
     * **/
    public static  void main(String[] args){
        MyProxyTarget test = BeanHelper.getFrameBean(MyProxyTarget.class);
        if(test==null){
            log.info("sss fail");
        }
        test.p1();
        log.info("---------------");
        test.p2();
        log.info("---------------");
        test.p3();
        log.info("---------------");
        test.p4();
    }
}
