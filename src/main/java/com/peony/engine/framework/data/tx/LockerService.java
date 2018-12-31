package com.peony.engine.framework.data.tx;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.netEvent.NetEventService;
import com.peony.engine.framework.data.OperType;
import com.peony.engine.framework.data.cache.CacheCenter;
import com.peony.engine.framework.data.cache.CacheEntity;
import com.peony.engine.framework.security.MonitorNumType;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by apple on 16-8-21.
 * 事务提交时的锁控制,运行在一个服务器中(main服务器)
 * 主要是对要提交的对象进行加锁并进行版本校验
 */
@Service(init = "init")
public class LockerService {
    private static final Logger log = LoggerFactory.getLogger(LockerService.class);
    private static final int maxTryLockTimes = 20;

    StringLocker stringLocker = new StringLockerReentrantLock();

    private CacheCenter cacheCenter;

    private NetEventService netEventService;
    private MonitorService monitorService;

    public void init(){
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
        cacheCenter= BeanHelper.getFrameBean(CacheCenter.class);
    }
    ////---------------------------外部代用
    public boolean lockAndCheckKeys(LockerData... lockerDatas){
        Arrays.sort(lockerDatas); //先做个排序 防止死锁
        return receiveLockRequest(lockerDatas); // 需要同步发送
    }
    public void unlockKeys(String... keys){
        receiveUnLockRequest(keys);
    }
    // TODO 如果确保每个服务在自己返回的时候，即使发生异常，也会把加的锁释放
    // TODO 有没有必要先做个排序，防止死锁
    // TODO: 2016/10/27 这里后面选择抛异常吧
    public boolean lockKeys(String... keys){
        if(keys == null || keys.length==0){
            log.warn("do lockKeys,but keys = "+keys);
            return true;
        }
        Arrays.sort(keys); //先做个排序 防止死锁
        return receiveLockKeys(keys);
    }

    public <T> T doLockTask(LockTask<T> task, String... keys) {
        boolean lockResult = lockKeys(keys);
        if(!lockResult){
            throw new MMException("lock error");
        }
        try {
            return task.run();
        }catch (Throwable e){
            throw new MMException(e);
        }finally {
            unlockKeys(keys);
        }
    }
    //// ----------------------------------外部代用end
    public boolean receiveLockKeys(String[] keys){
        boolean result = true;
        String failKey = null;
        for (String key : keys) {
            if(!stringLocker.lock(key)){
                result = false;
                failKey = key;
                break;
            }
        }
        if(!result){ // 如果加锁失败,已经加锁的要解锁
            for (String key : keys) {
                stringLocker.unlock(key);
                if(key.equals(failKey)){
                    break;
                }
            }
        }
        return result;
    }
    public boolean receiveLockRequest(LockerData[] lockerDatas){
        boolean result = true;
        String failKey = null;
        for (LockerData lockerData : lockerDatas) {
            if(!lockAndCheck(lockerData.getKey(),lockerData.getOperType(),lockerData.getCasUnique())){
                result = false;
                failKey = lockerData.getKey();
                log.warn("lock fail ,failKey = {}",failKey);
                break;
            }
        }
        if(!result){ // 如果加锁失败,已经加锁的要解锁
            for (LockerData lockerData : lockerDatas) {
                stringLocker.unlock(lockerData.getKey());
                if(lockerData.getKey().equals(failKey)){
                    break;
                }
            }
        }
        return result;
    }
    public void receiveUnLockRequest(String[] keys){
        for(String key : keys){
            stringLocker.unlock(key);
        }
    }
    /**
     * 加锁并校验
     * 校验规则:
     * update要进行版本校验
     * insert要进行有无校验
     * delete都ok
     * @param key
     * @param operType
     * @param casUnique
     */
    private boolean lockAndCheck(String key, OperType operType, long casUnique){
        if(!stringLocker.lock(key)){
            return false;
        }
        if(operType == OperType.Update){
            CacheEntity cacheEntity = cacheCenter.get(key); // 如果这个是从本地获取的,显然就没有更新过,so,没问题
            if(cacheEntity!= null && cacheEntity.getState() != CacheEntity.CacheEntityState.Normal){
                return false;
            }
            if(cacheEntity.getCasUnique() != casUnique && casUnique!=-1){ // 没有更新过 这里没有考虑casUnique为-1的情况
                return false;
            }
        }else if(operType == OperType.Insert){
            CacheEntity cacheEntity = cacheCenter.get(key);
            if(cacheEntity!= null && cacheEntity.getState() == CacheEntity.CacheEntityState.Normal){
                return false;
            }
        }
        return true;
    }

    public int getLockingNum(){
        return stringLocker.getLockingNum();
    }

    public static class LockerData implements Serializable,Comparable<LockerData>{
        private String key;
        private OperType operType;
        private long casUnique;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public OperType getOperType() {
            return operType;
        }

        public void setOperType(OperType operType) {
            this.operType = operType;
        }

        public long getCasUnique() {
            return casUnique;
        }

        public void setCasUnique(long casUnique) {
            this.casUnique = casUnique;
        }

        @Override
        public int compareTo(LockerData o) {
            return key.compareTo(o.getKey());
        }
    }

    class StringLockerConHashMap implements StringLocker{

        private ConcurrentHashMap<String,String> lockers = new ConcurrentHashMap<>();
        private ThreadLocal<Map<String,Integer>> threadLocalLockers = new ThreadLocal<Map<String,Integer>>(){
            @Override
            protected Map<String, Integer> initialValue() {
                return new HashMap<>();
            }
        }; // 做可重入锁

        @Override
        public boolean lock(String key) {
            // 加锁
            String olderKey = lockers.putIfAbsent(key,key);
            int lockTime = 0;
            while(olderKey != null){ // 加锁失败,稍等再加
                Integer count = threadLocalLockers.get().get(key);
                if(count != null && count >0){
                    // 重入
                    threadLocalLockers.get().put(key,++count);
                    monitorService.addMonitorNum(MonitorNumType.DoLockSuccessNum,1);
                    return true;
                }
                if(lockTime++>maxTryLockTimes){
                    // 这个地方不能用异常,因为加锁失败,要清理之前加成功的锁:如果考虑用最终捕获异常来清理锁，也可以考虑，但还是不如这样
//                ExceptionHelper.handle(ExceptionLevel.Warn,"锁超时,key = "+key,null);
                    log.warn("锁超时,key = "+key+",olderKey="+olderKey);
                    monitorService.addMonitorNum(MonitorNumType.DoLockFailNum,1);
                    return false;
                }
                try {
                    Thread.sleep(10); // TODO 这个时间要随机
                    olderKey = lockers.putIfAbsent(key,key);
                }catch (InterruptedException e){
                    monitorService.addMonitorNum(MonitorNumType.DoLockFailNum,1);
                    throw new MMException("锁异常,key = "+key);
                }
            }
            threadLocalLockers.get().put(key,1);
            monitorService.addMonitorNum(MonitorNumType.DoLockSuccessNum,1);
            return true;
        }

        @Override
        public void unlock(String key) {
            Integer count = threadLocalLockers.get().get(key);
            if(count == null || count <=1){
                if(lockers.remove(key) != null){
                    threadLocalLockers.get().put(key,0);
                    monitorService.addMonitorNum(MonitorNumType.DoUnLockNum,1);
                }
            }else{
                threadLocalLockers.get().put(key,--count);
                monitorService.addMonitorNum(MonitorNumType.DoUnLockNum,1);
            }
        }

        @Override
        public int getLockingNum() {
            return lockers.size();
        }
    }

    class StringLockerReentrantLock implements StringLocker{
        final int lockerNum = 1<<14;
        final int spit = lockerNum-1;
        static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

        ReentrantLock[] lockers = new ReentrantLock[lockerNum];
        AtomicInteger lockingNum = new AtomicInteger();

        StringLockerReentrantLock(){
            for(int i=0;i<lockerNum;i++){
                lockers[i] = new ReentrantLock();
            }
        }

        @Override
        public boolean lock(String key){
            int hash = spread(key.hashCode());
            int index = hash & spit;
//            System.out.println("index:"+index);
            ReentrantLock lock = lockers[index];
            try {
                boolean ret = lock.tryLock(200, TimeUnit.MILLISECONDS);
                if(ret){
                    lockingNum.incrementAndGet();
                    monitorService.addMonitorNum(MonitorNumType.DoLockSuccessNum,1);
//                    System.out.println("lock success");
                }else{
                    monitorService.addMonitorNum(MonitorNumType.DoLockFailNum,1);
                    log.warn("lock fail ,key = {},",key);
                }
                return ret;
            } catch (InterruptedException e) {
//                e.printStackTrace();
                log.warn("lock InterruptedException fail ,key = {},",key,e);
                monitorService.addMonitorNum(MonitorNumType.DoLockFailNum,1);
                return false;
            }
        }

        public void unlock(String key){
            int hash = spread(key.hashCode());
            ReentrantLock lock = lockers[hash & spit];
            lock.unlock();
            lockingNum.decrementAndGet();
            monitorService.addMonitorNum(MonitorNumType.DoUnLockNum,1);
//            System.out.println("unlock success");
        }

        final int spread(int h) {
            return (h ^ (h >>> 16)) & HASH_BITS;
        }
        public int getLockingNum(){
            return lockingNum.get();
        }
    }

    interface StringLocker{
        boolean lock(String key);
        void unlock(String key);
        int getLockingNum();
    }

}
