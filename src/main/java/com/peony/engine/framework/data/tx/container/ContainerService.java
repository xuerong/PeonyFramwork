package com.peony.engine.framework.data.tx.container;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.data.tx.ITxLifeDepend;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.data.tx.TxCacheService;
import com.peony.engine.framework.security.exception.MMException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 * 提供一些容器，并控制容器的事务，并发一致性等行为
 *
 *
 * 当一个线程用到一些TxContainer时，将其放入txContainerThreadLocal中，这样事务结束时进行处理
 * 事务结束后，清理txContainerThreadLocal。
 *
 */
@Service(init = "init")
public class ContainerService implements ITxLifeDepend {

    private TxCacheService txCacheService;

    ThreadLocal<Set<TxContainer>> txContainerThreadLocal = new ThreadLocal<>();

    TxMap map = new TxHashMap();

    @Gm(id = "tx map test1")
    public void test1(boolean ex){
        txTest(ex);
    }

    @Tx
    public void txTest(boolean ex){
        System.out.println(map.size());

        String key = UUID.randomUUID().toString();
        map.put(key,"test1");
        System.out.println(map.size());

        System.out.println(map.keySet());
        System.out.println(map.readForUpdate("ecbb0e32-6de3-42fd-abdd-20498c95d12e"));
//        System.out.println(map.get("ecbb0e32-6de3-42fd-abdd-20498c95d12e"));
        if(ex){
            throw new MMException("ex");
        }
    }


    public void init(){
        txCacheService.registerTxLifeDepend(this);
    }

    public boolean isInTx(){
        return txCacheService.isInTx();
    }


    public void registerTxContainer(TxContainer txContainer){
        Set<TxContainer> txContainerSet = txContainerThreadLocal.get();
        if(txContainerSet == null){
            txContainerSet = new HashSet<>();
            txContainerThreadLocal.set(txContainerSet);
        }
        //
        txContainerSet.add(txContainer);
    }
    // 事务开始
    // 事务提交
    // 事务异常
    public void txBegin() {
        Set<TxContainer> txContainerSet = txContainerThreadLocal.get();
        if(txContainerSet != null && txContainerSet.size()>0){
            for(TxContainer txContainer : txContainerSet){
                txContainer.txBegin();
            }
        }
    }
    public void txCommitSuccess(){
        Set<TxContainer> txContainerSet = txContainerThreadLocal.get();
        if(txContainerSet != null && txContainerSet.size()>0){
            for(TxContainer txContainer : txContainerSet){
                txContainer.txCommitSuccess();
            }
            txContainerThreadLocal.remove();
        }

    }
    public void txExceptionFail(){
        Set<TxContainer> txContainerSet = txContainerThreadLocal.get();
        if(txContainerSet != null && txContainerSet.size()>0){
            for(TxContainer txContainer : txContainerSet){
                txContainer.txExceptionFail();
            }
            txContainerThreadLocal.remove();
        }
    }
}
