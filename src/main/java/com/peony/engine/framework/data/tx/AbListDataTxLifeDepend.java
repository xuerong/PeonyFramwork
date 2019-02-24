package com.peony.engine.framework.data.tx;


import java.util.ArrayList;
import java.util.List;

public abstract class AbListDataTxLifeDepend <T> implements ITxLifeDepend{
    protected ThreadLocal<List<T>> jobThreadLocal = new ThreadLocal<>();
    public boolean checkAndPut(T object){
        if(TxCacheServiceHolder.txCacheService.isInTx()){
            List<T> jobList = jobThreadLocal.get();
            if(jobList == null){
                jobList = new ArrayList<>();
                jobThreadLocal.set(jobList);
            }
            jobList.add(object);
            return true;
        }
        return false;
    }
    @Override
    public void txCommitSuccess(){
        txCommitFinish(true);
    }
    @Override
    public void txExceptionFail(){
        txCommitFinish(false);
    }

    void txCommitFinish(boolean success){
        List<T> list = jobThreadLocal.get();
        if(list == null){
            return;
        }
        jobThreadLocal.remove();
        if(!success){
            return;
        }
        for(T job:list){
            if(job == null){
                continue;
            }
            executeTxCommit(job);
        }
    }
    protected abstract void executeTxCommit(T object);
}
