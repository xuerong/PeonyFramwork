package com.peony.engine.framework.data.container;

import com.peony.engine.framework.control.annotation.Service;

import java.util.Set;

/**
 *
 * 提供一些容器，并控制容器的事务，并发一致性等行为
 */
@Service
public class ContainerService {
    ThreadLocal<Set<TxContainer>> txContainerThreadLocal = new ThreadLocal<>();

    public void txCommit(){
        Set<TxContainer> txContainerSet = txContainerThreadLocal.get();
        if(txContainerSet != null && txContainerSet.size()>0){
            for(TxContainer txContainer : txContainerSet){
                txContainer.txCommit();
            }
        }
    }
}
