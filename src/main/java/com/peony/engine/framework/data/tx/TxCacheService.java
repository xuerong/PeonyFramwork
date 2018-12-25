package com.peony.engine.framework.data.tx;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.OperType;
import com.peony.engine.framework.data.cache.CacheEntity;
import com.peony.engine.framework.data.cache.KeyParser;
import com.peony.engine.framework.data.entity.account.Account;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by a on 2016/8/12.
 * 在一个事务的service提交之前，把数据存储到这里，在提交的时候，提交到数据库
 * 方案如下：
 * insert：
 */
@Service(init = "init")
public class TxCacheService {
    private static final Logger log = LoggerFactory.getLogger(TxCacheService.class);

    // 注意，这里面的数据应该是乱序的，提交的时候应该是顺序的，所以要坐个sort
    // 这样写默认了初始化,所以不用再赋值给线程了
    private ThreadLocal<Map<String, PrepareCachedData>> cacheDatas = new ThreadLocal<Map<String, PrepareCachedData>>() {
        protected Map<String, PrepareCachedData> initialValue() {
            return new HashMap<String, PrepareCachedData>();
        }
    };
    /**
     * 三种可能：
     * 1 null：不需要加锁
     * 2 size == 0：所有更新的对象都需要加锁
     * 3 size > 0：里面存在的兑现更需要加锁
     */
    private final ThreadLocal<Set<Class<?>>> lockClasses = new ThreadLocal<Set<Class<?>>>();

    private ThreadLocal<TxState> txStates = new ThreadLocal<TxState>(){
        protected TxState initialValue() {
            return TxState.Absent;
        }
    };
    private ThreadLocal<Integer> txHierarchy = new ThreadLocal<Integer>(){
        protected Integer initialValue() {
            return 0;
        }
    };

    private LockerService lockerService;
    private DataService dataService;
    private AsyncService asyncService;
    private EventService eventService;

    public void init(){
        lockerService = BeanHelper.getServiceBean(LockerService.class);
        dataService = BeanHelper.getServiceBean(DataService.class);
        asyncService = BeanHelper.getServiceBean(AsyncService.class);
    }

    //    @Gm(id="测试tx-----------------")
    public void gmaaa(String uid){

        for(int i =0;i<100;i++){
            final int a = i;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    testTx(uid);
                }
            }){
                {

                    setName("thead-"+a);
                }
            }.start();
        }
    }

    @Tx(lock = true)
    public void testTx(String uid){
//        UserMixed userMixed = dataService.selectObject(UserMixed.class,"uid=?",uid);
//        if(userMixed == null) {
//            System.err.println(Thread.currentThread().getName()+"  meiyounadao ");
//            userMixed = new UserMixed();
//            userMixed.setUid(uid);
//            dataService.insert(userMixed);
//        }else{
//            System.err.println(Thread.currentThread().getName()+"  nadaole ");
//        }
//        userMixed.setAdRewardCount(userMixed.getAdRewardCount()+1);
//        System.err.println(Thread.currentThread().getName()+"  --- "+userMixed.getAdRewardCount());
//        dataService.update(userMixed);
    }



    public boolean isLockClass(Class<?> cls){
        Set<Class<?>> lockClassSet = lockClasses.get();
        if(lockClassSet == null){
            return false;
        }
        if(lockClassSet.size() == 0){
            return true;
        }
        return lockClassSet.contains(cls);
    }

    public boolean isInTx(){
        return txStates.get() == TxState.In;
    }
    public void setTXState(TxState txState){
        txStates.set(txState);
    }

    // 事务开始
    public void begin(boolean isTx, boolean isLock, Class<?>[] lockClass){
        if(!isTx){
            return;
        }
        txHierarchy.set(txHierarchy.get()+1); // 嵌套事务
        setTXState(TxState.In);
        if(!isLock){
            return;
        }
//        Set<Class<?>> classSet = new HashSet<Class<?>>();
        Set<Class<?>> classSet = lockClasses.get();
        if(classSet == null){
            classSet = new HashSet<Class<?>>();
            lockClasses.set(classSet);
        }
        if(lockClass!=null && lockClass.length >0 ){
            for(Class<?> cls : lockClass){
                classSet.add(cls);
            }
        }
    }



    // 事务结束
    public boolean after(boolean tx){
        if(!isInTx()){
            return true;
        }
        if(tx) {
            txHierarchy.set(txHierarchy.get() - 1);
        }
        int hierarchy = txHierarchy.get();
        if(hierarchy>0){
            return true;
        }
        if(hierarchy < 0){
            log.error("------------------------------------not impossble happen");
        }
        setTXState(TxState.Committing);
        boolean result = commit();
        setTXState(TxState.None);
        // todo 这个地方需要优化：因为事务的执行不一定是网络线程，最好是直接清除，不然cacheDatas会有很多
        // todo lockClasses同样，别忘了，如果清除，在获取使用的时候，空值要赋值
        // todo exception同
        Map<String, PrepareCachedData> map = cacheDatas.get();
        if(map != null){
            map.clear();
        }
        Set<Class<?>> classSet = lockClasses.get();
        if(classSet != null){
            classSet.clear();
        }
        dataService.clearCacheEntitys();
        eventService.txCommitFinish(result);
        return result;
    }

    public void exception(){
        txHierarchy.set(txHierarchy.get()-1);
        int hierarchy = txHierarchy.get();
        if(hierarchy <=0){
            setTXState(TxState.None);
            Map<String, PrepareCachedData> map = cacheDatas.get();
            if(map != null){
                map.clear();
            }
            Set<Class<?>> classSet = lockClasses.get();
            if(classSet != null){
                classSet.clear();
            }
            dataService.clearCacheEntitys();
            eventService.txCommitFinish(false);
            if(hierarchy < 0){
                log.error("------------------------------------not impossble happen");
            }
        }
    }

    /**
     * 事务提交
     * 若要加锁,则先加锁(main服加锁),若要验证,则加锁后验证,都加锁验证通过,然后提交
     * 将对象的key和对应的casUnique都提交给main服,其进行加锁和验证,
     */
    public boolean commit(){
        Map<String, PrepareCachedData> map = cacheDatas.get();
        if(map == null){
            return true;
        }
        // -- 加锁校验
        Set<Class<?>> lockClass = lockClasses.get();
        List<LockerService.LockerData> lockerDataList = null;
        if(lockClass != null){ // 说明要加锁
            // 提取要加锁的对象
            for (Map.Entry<String, PrepareCachedData> entry:map.entrySet()){
                PrepareCachedData data = entry.getValue();
                if(data.getOperType() != OperType.Select && isLockClass(data.getData().getClass())){
                    LockerService.LockerData lockerData = new LockerService.LockerData();
                    lockerData.setKey(data.getKey());
                    lockerData.setOperType(data.getOperType());
                    long casUnique = -1;
                    CacheEntity older = dataService.getCacheEntity(data.getKey());
                    if(older != null){
                        casUnique = older.getCasUnique();
                    }
                    lockerData.setCasUnique(casUnique);
                    if(lockerDataList == null){
                        lockerDataList = new ArrayList<>();
                    }
                    lockerDataList.add(lockerData);
                }
            }
            if(lockerDataList!=null && lockerDataList.size()>0){
                boolean result = lockerService.lockAndCheckKeys(lockerDataList.toArray(new LockerService.LockerData[lockerDataList.size()]));
                if(!result){ // 加锁校验失败,提交也就失败
                    return false;
                }
            }
        }
        // --- 提交事务,无论中间出现什么情况,都要解锁
        // 先更新缓存，再统一提交异步服务器
        try{
            List<AsyncService.AsyncData> asyncDataList = null;
            for(PrepareCachedData data : map.values()){
                Object old = null;
                switch (data.getOperType()){
                    case Insert:
                        dataService.insert(data.getData(),false); // 这个地方用这种方式提交,如果有需要,可以换方式
                        break;
                    case Update:
                        old = dataService.update(data.getData(),false);
                        break;
                    case Delete:
                        dataService.delete(data.getData(),false);
                        break;
                }
                if(data.getOperType() == OperType.Insert || data.getOperType() == OperType.Update ||
                        data.getOperType() == OperType.Delete) {
                    if (asyncDataList == null) {
                        asyncDataList = new ArrayList<>();
                    }
                    AsyncService.AsyncData asyncData = new AsyncService.AsyncData();
                    asyncData.setOperType(data.getOperType());
                    asyncData.setKey(data.getKey());
                    asyncData.setObject(data.getData());
                    if(old != null) {
                        asyncData.setOld(((CacheEntity) old).getEntity());
                    }
                    asyncDataList.add(asyncData);
                }
            }

            if(asyncDataList!=null && asyncDataList.size()>0){
                // 提交异步服务器
                asyncService.asyncData(asyncDataList);
            }
        }finally {
            // -- 解锁
            if(lockerDataList!=null){
                int size = lockerDataList.size();
                if(size > 0){
                    String[] keys = new String[size];
                    int i=0;
                    for(LockerService.LockerData lockerData : lockerDataList){
                        keys[i++] = lockerData.getKey();
                    }
                    lockerService.unlockKeys(keys);
                }
            }
        }
        return true;
    }


    public PrepareCachedData get(String key){
        return cacheDatas.get().get(key);
    }

    public <T> List<T> replaceCacheObjectToList(String listKey,List<T> objectList,LinkedHashSet<String> keys){
        Map<String, PrepareCachedData> map = cacheDatas.get();
        if(map.size() > 0){
            Map<String,Integer> keyMap = null;
            List<Integer> deleteIndex = null;
            for (String key:map.keySet()) {
                PrepareCachedData prepareCachedData = map.get(key);
                if(!KeyParser.isObjectBelongToList(prepareCachedData.getData(),listKey)){
                    if(keys!= null && prepareCachedData.getOperType() == OperType.Update){
                        if(keys.contains(key)){
                            if(keyMap == null) {
                                keyMap = new HashMap<>();
                                int i = 0;
                                for (T t:objectList) {
                                    keyMap.put(KeyParser.parseKey(t),i++);
                                }
                            }
                            Integer index  = keyMap.get(key);
                            if(index != null) {
                                if(deleteIndex == null){
                                    deleteIndex = new ArrayList<>();
                                }
                                deleteIndex.add(index);
                            }
                        }
                    }
                    continue;
                }
                // 替换，或添加，或删除
                if(keyMap == null &&
                        prepareCachedData.getOperType() != OperType.Select) {
                    keyMap = new HashMap<>();
                    int i = 0;
                    for (T t:objectList) {
                        keyMap.put(KeyParser.parseKey(t),i++);
                    }
                }
                // 替换和添加,如果objectMap中存在,都要替换
                if(keyMap != null) {
                    Integer index = keyMap.get(key);
                    if(index != null) {
                        if(prepareCachedData.getOperType() == OperType.Update
                                || prepareCachedData.getOperType() == OperType.Insert){
//                            objectList.remove((int)index);
                            objectList.set(index,(T)prepareCachedData.getData());
                            if(prepareCachedData.getOperType() == OperType.Insert){
                                log.warn("insert object in this threadLocalCache is exist in objectList ,objectKey = "
                                        +key+",listKey = "+listKey);
                            }
                        }else if(prepareCachedData.getOperType() == OperType.Delete){
                            if(deleteIndex == null){
                                deleteIndex = new ArrayList<>();
                            }
                            deleteIndex.add(index);
                        }
                    }else{
                        if(prepareCachedData.getOperType() == OperType.Insert
                                || prepareCachedData.getOperType() == OperType.Update){
                            objectList.add((T)prepareCachedData.getData());
                            if(prepareCachedData.getOperType() == OperType.Update){
//                                log.warn("update object in this threadLocalCache is not exist in objectList ,objectKey = "
//                                        +key+",listKey = "+listKey);
                            }
                        }
                    }
                }

            }
            //
            if(deleteIndex!= null){

                if(deleteIndex.size() > 1000){
                    // 这个删除太多的话，用新方式
                    log.warn("if here happen,deleteIndex.size()={}",deleteIndex.size());
                    deleteIndex.sort((o1,o2)->o1-o2);

                    List<T> ret = new ArrayList(objectList.size() - deleteIndex.size());

                    int begin = 0;
                    for(Integer index : deleteIndex){
                        if(index <=begin){
                            begin = index+1;
                            continue;
                        }
                        for(int i=begin;i<index;i++){
                            ret.add(objectList.get(i));
                        }
                        begin = index+1;
                    }
                    for(int i=begin,len = objectList.size();i<len;i++){
                        ret.add(objectList.get(i));
                    }
                    return ret;
                }else{
                    deleteIndex.sort((o1,o2)->o2-o1);
                    for(Integer index : deleteIndex){
                        objectList.remove(index.intValue());
                    }
                }
            }
        }
        return objectList;
    }

    public boolean putWhileSelect(String key,Object entity){
        Map<String, PrepareCachedData> map = cacheDatas.get();
        PrepareCachedData prepareCachedData = new PrepareCachedData();
        prepareCachedData.setData(entity);
        prepareCachedData.setKey(key);
        prepareCachedData.setOperType(OperType.Select);
        PrepareCachedData older = map.put(key,prepareCachedData);
        if(older!= null){
            log.warn("older != null while insert key   =  "+key);
        }
        return true;
    }
    /**
     * 插入一个对象，
     */
    public boolean insert(String key,Object entity){
        Map<String, PrepareCachedData> map = cacheDatas.get();
        PrepareCachedData older = map.get(key);
        if(older != null && older.getOperType() != OperType.Delete){
            throw new MMException("object is exist while insert object key = "+key);
        }
        PrepareCachedData prepareCachedData = new PrepareCachedData();
        prepareCachedData.setData(entity);
        prepareCachedData.setKey(key);
        prepareCachedData.setOperType(OperType.Insert);
        map.put(key,prepareCachedData);
        return true;
    }
    /**
     * 更新一个对象，
     * TODO 一次get一次put是否可以合并
     */
    public Object update(String key,Object entity){
        Map<String, PrepareCachedData> map = cacheDatas.get();
        PrepareCachedData older = map.get(key);
        if(older != null && older.getOperType() == OperType.Delete){
            throw new MMException("object has deleted while update object key = "+key);
        }
        PrepareCachedData prepareCachedData = new PrepareCachedData();
        prepareCachedData.setData(entity);
        prepareCachedData.setKey(key);
        prepareCachedData.setOperType(OperType.Update);
        PrepareCachedData old = map.put(key,prepareCachedData);
        if(old != null && old.getOperType() == OperType.Insert){
            // 插入的，则更新变成插入
            prepareCachedData.setOperType(OperType.Insert);
        }
        return old==null?null:old.getData();
    }
    /**
     * 删除一个实体
     * 由于要异步删除，缓存中设置删除标志位,所以，在缓存中是update
     */
    public boolean delete(String key,Object entity){
        Map<String, PrepareCachedData> map = cacheDatas.get();
        PrepareCachedData prepareCachedData = new PrepareCachedData();
        prepareCachedData.setData(entity);
        prepareCachedData.setKey(key);
        prepareCachedData.setOperType(OperType.Delete);
        map.put(key,prepareCachedData);
        return true;
    }
    /**
     * 删除一个实体,condition必须是主键
     */
//    public <T> boolean delete(Class<T> entityClass, String condition, Object... params){
//        return false;
//    }

    public static class PrepareCachedData {
        private OperType operType;
        private String key;
        private Object data;

        public OperType getOperType() {
            return operType;
        }

        public void setOperType(OperType operType) {
            this.operType = operType;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
