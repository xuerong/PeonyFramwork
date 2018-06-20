package test.proxyTest;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.aop.annotation.AspectMark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 2015/11/17.
 */
@Service
public class MyProxyTarget {
    private static final Logger log = LoggerFactory.getLogger(MyProxyTarget.class);
    @AspectMark(mark = {"aa"})
    public void p1(){
        log.info("test proxy-aa");
    }
    @AspectMark(mark = {"bb"})
    public void p2(){
        log.info("test proxy-bb");
    }

    @AspectMark(mark = {"aa","bb"})
    public void p3(){
        log.info("test proxy-aa,bb");
    }
    @AspectMark(mark = {"cc"})
    public void p4(){
        log.info("test proxy-cc");
    }
}
