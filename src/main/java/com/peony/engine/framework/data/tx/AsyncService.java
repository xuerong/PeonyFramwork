package com.peony.engine.framework.data.tx;

import com.appPacket.TestDbEntity;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.control.netEvent.NetEventService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.data.OperType;
import com.peony.engine.framework.data.cache.CacheCenter;
import com.peony.engine.framework.data.cache.CacheEntity;
import com.peony.engine.framework.data.cache.KeyParser;
import com.peony.engine.framework.data.persistence.orm.DataSet;
import com.peony.engine.framework.data.persistence.orm.EntityHelper;
import com.peony.engine.framework.security.MonitorNumType;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by apple on 16-8-14.
 * 更新数据库检测用一个线程，然后分配给其它线程去处理
 * 注意：原来同一个服务线程传过来的更新需求，要在同一个更新线程中按顺序处理
 */
@Service(init = "init",destroy = "destroy",destroyPriority = 5)
public class AsyncService {
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);
    // 异步更新队列
//    private static LinkedBlockingQueue<AsyncData> asyncDataQueue = new LinkedBlockingQueue<AsyncData>();
    // 另一个队列，key为对象的类的名字,只存储增加和删除，根据异步对象的类型进行存储，在REFRESHDBLIST中起作用：
    // 1防止漏掉数据：插入数据库之后才删它，而asyncDataQueue在插入数据库之前就会被删掉了，2提高查询效率
    private ConcurrentHashMap<String,Map<AsyncData,AsyncData>> asyncDataMap = new ConcurrentHashMap<>();
    private final int threadCount = 10;//Runtime.getRuntime().availableProcessors()+1;
    private Random threadRand = new Random();
//    private ThreadLocal<Integer> threadNum = new ThreadLocal<>();
    private Map<Integer,Worker> workerMap = new HashMap<>();
    // listKeys
    // key为对象的类的名字，值为其对应的listKeys
//    private ConcurrentHashMap<String,Set<String>> listKeysMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Class<?>,Map<String,Map<String,String>>> listKeyIndex = new ConcurrentHashMap<>();
    //
    private CacheCenter cacheCenter;
    private NetEventService netEventService;
    private LockerService lockerService;
    private MonitorService monitorService;
    //监控数据

    public void init(){
        netEventService = BeanHelper.getServiceBean(NetEventService.class);
        cacheCenter= BeanHelper.getFrameBean(CacheCenter.class);
        lockerService = BeanHelper.getServiceBean(LockerService.class);
        // 判断本服务器是否是异步服务器，如果是，则启动异步更新线程
        if(Server.getEngineConfigure().isAsyncServer()){
            startAsyncService();
        }
    }

    public void destroy(){
        stop();
    }

    private void startAsyncService(){
        for(int i=0;i<threadCount;i++){
            Worker worker = new Worker(i);
            workerMap.put(i,worker);
            worker.start();
        }
    }

    /**
     * 服务器停止之前别忘了调用该方法
     * 由于要等待各个线程处理完成，所以可能要等待一段时间
     */
    public void stop(){
        if(!Server.getEngineConfigure().isAsyncServer()){
            return;
        }
        CountDownLatch latch = new CountDownLatch(workerMap.size());
        for(Worker worker : workerMap.values()){
            worker.stop(latch);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new MMException(e);
        }
    }

    /**
     * 插入一个对象，
     *
     */
    public void insert(String key,Object entity){
        doAsyncData(key,entity,OperType.Insert);
    }
    /**
     * 更新一个对象，
     */
    public void update(String key,Object entity){
        doAsyncData(key,entity,OperType.Update);
    }
    /**
     * 删除一个实体
     * 由于要异步删除，缓存中设置删除标志位,所以，在缓存中是update
     */
    public void delete(String key,Object entity){
        doAsyncData(key,entity,OperType.Delete);
    }
    private void doAsyncData(String key,Object object,OperType operType){
        AsyncData asyncData = new AsyncData();
        asyncData.setOperType(operType);
        asyncData.setKey(key);
        asyncData.setObject(object);
        // 这个同一个服务线程的threadNum必须一样
        // TODO 这个线程num应该根据对象的key的hascode来决定吧
//        Integer t = threadNum.get();
//        if(t == null){
//            t = threadRand.nextInt(threadCount);
//            threadNum.set(t);
//        }
        asyncData.setThreadNum(getThreadNum(key));
        receiveAsyncData(asyncData);
    }

    private int getThreadNum(String key){
        if(key == null){
            log.error("key == null,");
            return threadRand.nextInt(threadCount);
        }
        return Math.abs(key.hashCode())%threadCount;
    }

    /**
     * 事务可以考虑用这个，这样，一个事务只需要一次网络访问
     */
    public void asyncData(List<AsyncData> asyncDataList){
//        Integer t = threadNum.get();
//        if(t == null){
//            t = threadRand.nextInt(threadCount);
//            threadNum.set(t);
//        }
        for(AsyncData asyncData : asyncDataList){
            asyncData.setThreadNum(getThreadNum(asyncData.getKey()));
        }
        receiveAsyncData(asyncDataList);
    }
    /**
     * 从异步服务器中获取满足条件的对象
     * 当从数据库中获取list之后，需要从异步服务器中获取满足该list且还没有更新到数据库中的对象，主要是插入和删除的
     * 同时将对应的listKey记录到异步服务器中，以便新数据插入时更新对应的list
     */
    public List<AsyncData> getAsyncDataBelongListKey(Class<?> entityClass,String listKey){
        return receiveRefreshDBList(entityClass,listKey);
    }
    /// 上面的四个函数，处理其他服务器发送过来的异步数据库请求
    public void receiveAsyncData(Object object){
        if(object instanceof AsyncData){
            AsyncData asyncData = (AsyncData)object;
            doReceiveAsyncData(asyncData);
        }else if(object instanceof List){
            List<AsyncData> asyncDataList = (List<AsyncData>)object;
            for(AsyncData asyncData : asyncDataList){
                doReceiveAsyncData(asyncData);
            }
        }
    }

    // 其他服务器发送来的，异步服务器有的对应list的对象和更新状态
    public List<AsyncData> receiveRefreshDBList(Class<?> entityClass,String listKey){
        // 查看并插入listKeys插入
//        String classKey = KeyParser.getClassNameFromListKey(listKey);

        String classKey = entityClass.getName();

        Map<String,Map<String,String>> fieldMap = listKeyIndex.get(entityClass);
        String[] listKeyStrs = listKey.split(KeyParser.LISTSEPARATOR); // 类，list，field，value
        if(listKeyStrs.length<4 && listKeyStrs.length != 2){
            log.error("listKey is Illegal : listKey = "+listKey);
        }else {
            if (fieldMap == null) {
                fieldMap = new ConcurrentHashMap<>();
                Map<String, Map<String, String>> old = listKeyIndex.putIfAbsent(entityClass, fieldMap);
                if (old != null) {
                    fieldMap = old;
                }
            }
            String fieldKey = listKeyStrs.length == 2?"":listKeyStrs[2];
            Map<String, String> valueMap = fieldMap.get(fieldKey);
            if (valueMap == null) {
                valueMap = new ConcurrentHashMap<>();
                Map<String, String> old = fieldMap.putIfAbsent(fieldKey, valueMap);
                if (old != null) {
                    valueMap = old;
                }
            }
            if (listKeyStrs.length == 2) { // 全表
                valueMap.putIfAbsent("", listKey);
            } else {
                valueMap.putIfAbsent(listKeyStrs[3],listKey);
            }
        }

//        Set<String> listKeys = listKeysMap.get(classKey);
//        if(listKeys == null){
//            listKeys = new ConcurrentHashSet<>();
//            listKeysMap.putIfAbsent(classKey,listKeys);
//            listKeys = listKeysMap.get(classKey);
//        }
//        listKeys.add(listKey);// 注意这里一定要先插入，再获取再插入，而不能创建-插入-放入listKeysMap，多线程下回出错
        // 从异步列表中获取相应的对象
        List<AsyncData> result = null;
        Map<AsyncData,AsyncData> asyncDataList = asyncDataMap.get(classKey);
        if(asyncDataList != null){
            Iterator<AsyncData> iterator = asyncDataList.values().iterator();
            while (iterator.hasNext()){
                AsyncData asyncData = iterator.next();
                if(KeyParser.isObjectBelongToList(asyncData.getObject(),listKey)){
                    if(result == null){
                        result = new ArrayList<>();
                    }
                    result.add(asyncData);
                }
            }
        }
        return result;
    }
    /**
     * 返回是否移除该listkey
     * @param asyncData
     * @param listKey
     * @return
     */
    private boolean updateCacheList(AsyncData asyncData, String listKey){
        return updateCacheList(asyncData, listKey,0);
    }
    private boolean updateCacheList(AsyncData asyncData, String listKey,int oper){
        if (!lockerService.lockKeys(listKey)) { // 要不要做成一起加锁， 解锁，增加效率？
            throw new MMException("加锁失败,listKey = " + listKey);
        }
        // 从缓存中取数据
        CacheEntity cacheEntity = cacheCenter.get(listKey);
        if (cacheEntity == null) {
            // 缓存中没有，删除掉这个listKey
            lockerService.unlockKeys(listKey);
            return true;
        }
        List<String> keyList = (List<String>) cacheEntity.getEntity();
        if (asyncData.getOperType() == OperType.Insert){
//                            if (!keyList.contains(asyncData.getKey())) // TODO 这里先不判断，影响效率，或者使用Set，会影响排序，这里要考虑一下了。。。。。。。。。，可以用LinkedHashSet 6666
            keyList.add(asyncData.getKey());
        }else if (asyncData.getOperType() == OperType.Delete){
            keyList.remove(asyncData.getKey());
        }else if(asyncData.getOperType() == OperType.Update){
            if(oper == 1){
                keyList.add(asyncData.getKey());
            }else if(oper == 2){
                keyList.remove(asyncData.getKey());
            }
        }
        cacheCenter.update(listKey,cacheEntity); // 由于加了锁之后获取的，所以不用担心版本问题，第一次放入缓存的地方也加了锁
        lockerService.unlockKeys(listKey);
        return false;
    }

    private void doReceiveAsyncData(AsyncData asyncData){
        if(asyncData.getOperType() == OperType.Insert || asyncData.getOperType() == OperType.Delete) {
            Map<AsyncData,AsyncData> asyncDataList = asyncDataMap.get(asyncData.getObject().getClass().getName());
            if (asyncDataList == null) {
                // TODO 这里用这个list怎么样呢，有没有更好的选择？因为后面有删除需求，这个删除在多线程会不会效率太低
                asyncDataList = new ConcurrentHashMap<>();//Collections.synchronizedList(new LinkedList<AsyncData>());
                asyncDataMap.putIfAbsent(asyncData.getObject().getClass().getName(), asyncDataList);
                asyncDataList = asyncDataMap.get(asyncData.getObject().getClass().getName());
            }
            asyncDataList.put(asyncData,asyncData);
        }
        Worker worker = workerMap.get(asyncData.getThreadNum());
        boolean success = worker.addAsyncData(asyncData);
        // 插入可能存在的listKey，修改非主键的listkey
        Map<String,Map<String,String>> fieldMap = listKeyIndex.get(asyncData.getObject().getClass());
        if(fieldMap != null) {
            Map<String,Method> getMethodMap = null;
            for (Map.Entry<String, Map<String, String>> entry : fieldMap.entrySet()) {
                if (entry.getKey().equals("")) { // 是全表
                    String listKey = entry.getValue().get("");
                    boolean remove = updateCacheList(asyncData, listKey);
                    if (remove) {
                        fieldMap.remove(entry.getKey());
                    }
                } else {
                    if(asyncData.getOperType() == OperType.Insert || asyncData.getOperType() == OperType.Delete) {
                        String[] fieldNames = entry.getKey().split(KeyParser.SEPARATOR);
                        if(getMethodMap == null){
                            getMethodMap = EntityHelper.getGetMethodMap(asyncData.getObject().getClass());
                        }
                        String valueKey = getValueKey(fieldNames,getMethodMap,asyncData.getObject());
                        String listKey = entry.getValue().get(valueKey);
                        if(listKey != null){
                            boolean remove = updateCacheList(asyncData, listKey);
                            if (remove) {
                                entry.getValue().remove(valueKey);
                            }
                        }
                    }else if(asyncData.getOperType() == OperType.Update){
                        // 如果是主键的list，则不做处理
                        String[] fieldNames = entry.getKey().split(KeyParser.SEPARATOR);
                        Map<String,Method> pkGetMethodMap = EntityHelper.getPkGetMethodMap(asyncData.getObject().getClass());
                        boolean notPkList = false;
                        for(String fieldName : fieldNames){
                            if(!pkGetMethodMap.containsKey(fieldName)){
                                notPkList = true;
                                break;
                            }
                        }
                        if(notPkList){ // 非pk的list，如果有更新，需要检查是否有list更新
                            // 如果old的listkey和新的不一致，则需要修改
                            if(asyncData.getOld() != null) {
                                if (getMethodMap == null) {
                                    getMethodMap = EntityHelper.getGetMethodMap(asyncData.getObject().getClass());
                                }
                                String oldValueKey = getValueKey(fieldNames, getMethodMap, asyncData.getOld());
                                String valueKey = getValueKey(fieldNames, getMethodMap, asyncData.getObject());
                                if (!oldValueKey.equals(valueKey)) {
                                    // 删除旧的
                                    String oldListKey = entry.getValue().get(oldValueKey);
                                    if(oldListKey != null){
                                        boolean remove = updateCacheList(asyncData, oldListKey,2);
                                        if (remove) {
                                            entry.getValue().remove(valueKey);
                                        }
                                    }
                                    // 添加新的
                                    String listKey = entry.getValue().get(valueKey);
                                    if (listKey != null) {
                                        boolean remove = updateCacheList(asyncData, listKey,1);
                                        if (remove) {
                                            entry.getValue().remove(valueKey);
                                        }
                                    }
                                    System.out.println("oldValueKey:"+oldValueKey+",valueKey:"+valueKey+",oldListKey:"+",listKey:"+listKey);
                                }
                            }else{
                                log.error("old == null while update,asyncData = {}",asyncData);
                            }
                        }
                    }
                }
            }
        }




        if(!success){
            throw new MMException("更新数据库队列满，异步服务器压力过大");
        }
    }


    @Tx
    @Gm(id="testUpdateForList")
    public void testUpdateForList(){
        DataService dataService = BeanHelper.getServiceBean(DataService.class);
//        Random random = new Random();
//        for(int i = 0;i<1000;i++){
//            TestDbEntity testDbEntity = new TestDbEntity();
//            testDbEntity.setUid("testDbEntity"+i);
//            testDbEntity.setAaa(random.nextInt(50)+"");
//            testDbEntity.setBbb(random.nextInt(300)+"");
//            dataService.insert(testDbEntity);
//        }
        List<TestDbEntity> testDbEntities = dataService.selectList(TestDbEntity.class,"aaa=?",10+"");
        System.out.println(testDbEntities.size()+"    "+testDbEntities);
        testDbEntities = dataService.selectList(TestDbEntity.class,"aaa=?",11+"");
        System.out.println(testDbEntities.size()+"    "+testDbEntities);
        if(testDbEntities.size()>5){
            TestDbEntity testDbEntity = testDbEntities.get(4);
            testDbEntity.setAaa(10+"");
            dataService.update(testDbEntity);
        }
        testDbEntities = dataService.selectList(TestDbEntity.class,"aaa=?",10+"");
        System.out.println(testDbEntities.size()+"    "+testDbEntities);
        testDbEntities = dataService.selectList(TestDbEntity.class,"aaa=?",11+"");
        System.out.println(testDbEntities.size()+"    "+testDbEntities);
    }

    private String getValueKey(String[] fieldNames,Map<String,Method> getMethodMap,Object object){
        StringBuilder valueSb = new StringBuilder();
        try {
            for (String fieldName : fieldNames) {
                Method method = getMethodMap.get(fieldName);
                if (method == null) {
                    log.error("listKey is Illegal : fieldName is not exist in getMethodMap , fieldName = " + fieldName);
                }
                if(valueSb.length()>0){
                    valueSb.append(KeyParser.SEPARATOR);
                }
                valueSb.append(KeyParser.parseParamToString(method.invoke(object)));
            }
        } catch (IllegalAccessException |InvocationTargetException e) {
            log.error("listKey is Illegal while invoke getMethodMap,entityClass={}",object.getClass(),e);
        }
        String valueKey = valueSb.toString();
        return valueKey;
    }


    public static class AsyncData{
        private String key;
        private OperType operType;
        private Object object;
        private Object old;

        private int threadNum; // 更新用的线程编号，同一个服务中的更新必须是同一个threadNum

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

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public int getThreadNum() {
            return threadNum;
        }

        public void setThreadNum(int threadNum) {
            this.threadNum = threadNum;
        }

        public Object getOld() {
            return old;
        }

        public void setOld(Object old) {
            this.old = old;
        }

        @Override
        public String toString(){


            Map<String,Method> methodMap = EntityHelper.getGetMethodMap(object.getClass());
            StringBuilder sb = null;
            if(methodMap != null){
                sb = new StringBuilder();
                try {
                    for (Map.Entry<String, Method> entry : methodMap.entrySet()) {
                        Object value = entry.getValue().invoke(object);
                        sb.append(entry.getKey()).append("=").append(value).append(";");
                    }
                }catch (Throwable e){
                    log.error(object.getClass()+" get method error!",e);
                }
            }else{
                log.error("methodMap == null,why ! class = "+object.getClass());
            }

            return new StringBuffer("key="+key).
                    append(",object="+(sb==null?object:sb.toString())).
                    append(",operType="+operType).
                    append(",threadNum"+threadNum).
                    toString();
        }
    }

    public class Worker{
        private static final int MAXSWALLOWSTOPEXCEPTIONTIMES = 10; // 停止时最大吞掉异常的次数
        private static final int MAXWAITTIMES = 100; // 停止时最多等待次数
        private static final int WAITINTERVAL = 200; // 停止时每次等待时间
        private static final int MAXDBTIMES = 3; // 最大提交数据库次数
        private static final int WAITINTERVALDB = 1000; //提交失败等待时间

        private LinkedBlockingQueue<AsyncData> asyncDataQueue = new LinkedBlockingQueue<AsyncData>();
        private Thread dbThread = null;
        private int threadNum;
        private volatile boolean running = false;

        public boolean isRunning(){
            return running;
        }

        public Worker(int threadNum){
            this.threadNum = threadNum;
        }

        public boolean addAsyncData(AsyncData asyncData){
            if(running){
                return asyncDataQueue.offer(asyncData);
            }else{
                log.warn("异步服务器已经停止运行，或还没有运行,asyncData:"+asyncData.toString());
                return asyncDataQueue.offer(asyncData);
//                throw new MMException("异步服务器已经停止运行，或还没有运行,asyncData:"+asyncData.toString());

            }
        }


        public void start(){
            running = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (true){
                        AsyncData asyncData = null;
                        try{
                            asyncData = asyncDataQueue.take();
                            boolean success = false;
                            int dbTime = 0;
                            while(!success){
                                try {
                                    switch (asyncData.getOperType()){
                                        case Insert:
                                            DataSet.insert(asyncData.getObject());
                                            monitorService.addMonitorNum(MonitorNumType.InsertSqlNum,1);
                                            break;
                                        case Update:
                                            DataSet.update(asyncData.getObject(),EntityHelper.parsePkCondition(asyncData.getObject()));
                                            monitorService.addMonitorNum(MonitorNumType.UpdateSqlNum,1);
                                            break;
                                        case Delete:
                                            DataSet.delete(asyncData.getObject(),EntityHelper.parsePkCondition(asyncData.getObject()));
                                            monitorService.addMonitorNum(MonitorNumType.DeleteSqlNum,1);
                                            break;
                                    }
                                    success = true;
                                } catch (Exception e) {
                                    if(dbTime++ >= MAXDBTIMES){
                                        log.error("提交数据库失败，共尝试次数："+dbTime);
                                        break;
                                    }
                                    log.error("提交数据库失败，"+WAITINTERVALDB+"毫秒后尝试再次提交，尝试次数："+dbTime);
                                    Thread.sleep(WAITINTERVALDB);
                                } finally {

                                }
                            }
                            if(success){ // 删除记录 TODO 这里删除是不是有点慢
                                monitorService.addMonitorNum(MonitorNumType.SuccessSqlNum,1);
                                Map<AsyncData,AsyncData> asyncDataList = asyncDataMap.get(asyncData.getObject().getClass().getName());
                                if(asyncDataList != null) {
                                    asyncDataList.remove(asyncData);
                                }
                            }
                        }catch (Throwable e){
                            if(e instanceof InterruptedException && asyncData == null && !running){
                                // stop发生了
                                log.info("async thread stop success,threadNum="+threadNum);
                                break;
                            }
                            // 这里失败怎么办
                            asyncDbFail(asyncData);
                            e.printStackTrace();
                        }

                    }
                }
            };
            dbThread = new Thread(runnable);
            dbThread.start();
        }
        // TODO 更新失败的处理
        private void asyncDbFail(AsyncData asyncData){
            monitorService.addMonitorNum(MonitorNumType.FailSqlNum,1);
            log.error("asyncDbFail,"+asyncData);
        }
        // todo 这里用毒丸方法如何？
        public void stop(final CountDownLatch countDownLatch){
            running = false; // 不再接收处理
            new Thread(){
                @Override
                public void run(){
                    int SwallowExceptionTime = 0;
                    int waitTime = 0;
                    int lastSize = asyncDataQueue.size();
                    if(lastSize > 0 && !dbThread.isAlive()){
                        log.info("restart async thread---");
                        dbThread.start(); // 重新启动更新线程
                    }
                    while(!asyncDataQueue.isEmpty()){
                        try {
                            int size = asyncDataQueue.size();
                            if(waitTime++>MAXWAITTIMES && size >= lastSize){
                                log.error("停止异步服务器出现异常，在指定时间内未处理完，并且至少"+WAITINTERVAL+"毫秒内没有处理数据，workerId = "+threadNum
                                        +",剩余未处理数据量："+asyncDataQueue.size());
                                while(asyncDataQueue.size() > 0){
                                    log.info(asyncDataQueue.take().toString());
                                }
                                break;
                            }
                            lastSize = size;
                            Thread.sleep(WAITINTERVAL);
                        }catch (InterruptedException e){
                            if(SwallowExceptionTime++ < MAXSWALLOWSTOPEXCEPTIONTIMES){
                                continue;
                            }
                            log.error("停止异步服务器出现异常，多次出现打断异常，workerId = "+threadNum
                                    +",剩余未处理数据量："+asyncDataQueue.size());
                            break;
                        }
                    }
                    // 打断循环提交线程，如果能够被打断，是不是说明没有最后一个在处理的数据?
                    dbThread.interrupt();
                    countDownLatch.countDown();
                }
            }.start();
        }

    }
}
