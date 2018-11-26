package com.peony.engine.framework.data.tx;

import com.peony.engine.framework.control.aop.AspectProxy;
import com.peony.engine.framework.control.aop.annotation.Aspect;
import com.peony.engine.framework.control.aop.annotation.AspectOrder;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Created by apple on 16-8-21.
 * 对四种服务进行切面,查看对应的函数是否添加了事务
 *
 */
@AspectOrder(value = 0) // 事务要优先执行
@Aspect(
        annotation = {Tx.class
        }
)
public class TxProxy extends AspectProxy {
    private static final Logger log = LoggerFactory.getLogger(TxProxy.class);
    private TxCacheService txCacheService;
    // 在构造函数中添加恐怕有问题
//    public TxProxy(){
//        txCacheService = BeanHelper.getServiceBean(TxCacheService.class);
//    }
    @Override
    public void before(Object object,Class<?> cls, Method method, Object[] params) {
        Tx tx = method.getAnnotation(Tx.class);
        if(txCacheService == null){
            // 这个地方不用加锁，因为多个线程获取的都是同一个
            txCacheService = BeanHelper.getServiceBean(TxCacheService.class);
        }
        txCacheService.begin(tx.tx(),tx.lock(),tx.lockClass());
//        log.info("----------------------------before-----"+cls+","+method+","+txCacheService.isInTx());
    }

    @Override
    public void after(Object object,Class<?> cls, Method method, Object[] params, Object result) {
//        log.info("----------------------------after------"+cls+","+method+","+txCacheService.isInTx());
        Tx tx = method.getAnnotation(Tx.class);
        boolean success = txCacheService.after(tx.tx());
        //
        if(success){ // 没有事务或事务提交成功

        }else{
            // 事务提交失败，要重新执行该事务
            if(tx.tx()) {
                throw new MMException(MMException.ExceptionType.TxCommitFail, "tx commit fail");
            }
        }
    }

    @Override
    public void exceptionCatch(Throwable e) {
        txCacheService.exception();
    }
}
