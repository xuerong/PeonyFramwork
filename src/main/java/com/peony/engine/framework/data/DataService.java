package com.peony.engine.framework.data;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.cache.CacheEntity;
import com.peony.engine.framework.data.cache.CacheService;
import com.peony.engine.framework.data.cache.KeyParser;
import com.peony.engine.framework.data.persistence.dao.DataAccessor;
import com.peony.engine.framework.data.persistence.dao.DatabaseHelper;
import com.peony.engine.framework.data.persistence.orm.DataSet;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.tx.AsyncService;
import com.peony.engine.framework.data.tx.LockerService;
import com.peony.engine.framework.data.tx.TxCacheService;
import com.peony.engine.framework.security.MonitorNumType;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.helper.ClassHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by a on 2016/8/10.
 * 这个是操作数据的对外接口
 * 对于对对象的增删该查，都是对缓存和数据库的操作
 *
 * 注意：这里面锁操作的对象都必须注解：DBEntity
 * 如果仅对缓存进行操作，请用CacheCenter
 *
 * 这里仅提供这些方法
 *
 * 过程：
 * 如果处理的数据在事务进行中，则将数据交给事务线程缓存处理，
 * 否则，对缓存进行处理，对于get操作，缓存不存在要穿透到数据库进行查询，而flush操作则仅更新缓存，和异步数据库
 *
 *
 * 这里面的数据可以考虑不用返回(flush)而是出现错误抛出异常
 *
 * // 数据模块考虑
 * DataService
 * ThreadLocalCache
 * CacheCenter
 * DataSet
 * LockerServer
 * AsyncServer
 *
 * 关于异步insert(和delete)和list之间的处理:
 * 1、getList的缓存作一个标记,insert的时候查询标记来修改对应的缓存list(解决insert和和缓存list之间的问题)
 * 2、---数据库中getList,从异步中加载相应的多出的对象,放进缓存(解决异步insert和getList之间的为题)
 */
@Service(init = "init", initPriority = 2)
public class DataService {
    private static Logger logger = LoggerFactory.getLogger(DataService.class);
    private CacheService cacheService;
    private AsyncService asyncService;
    private LockerService lockerService;
    private TxCacheService txCacheService;
    private MonitorService monitorService;

    // 这里缓存一下查询结果,是保存一下查询的casUnique,这个只有在InTx且加锁的时候才使用
    // 当加锁之后,进行版本验证用
    private final ThreadLocal<Map<String,CacheEntity>> cacheEntitys = new ThreadLocal<Map<String,CacheEntity>>();

    public void init() {
        cacheService= BeanHelper.getServiceBean(CacheService.class);
        asyncService = BeanHelper.getServiceBean(AsyncService.class);
        lockerService = BeanHelper.getServiceBean(LockerService.class);
        txCacheService = BeanHelper.getServiceBean(TxCacheService.class);

        List<Class<?>> entities = ClassHelper.getClassListByAnnotation(DBEntity.class);
        for (Class<?> enty : entities) {
            if (!Serializable.class.isAssignableFrom(enty)) {
                throw new MMException("DBEntity 类型必须实现 Serializable 接口! " + enty.getSimpleName());
            }
        }

        // List<Class<?>> entities = ClassHelper.getClassListByAnnotation(DBEntity.class);
//        if("true".equals(Server.getEngineConfigure().getString("auto_create_table", "false"))){
//            DataAccessor dataAccessor = BeanHelper.getFrameBean(DataAccessor.class);
//            // 检查和更新数据库
//            for (Class<?> enty : entities) {
//                DBEntity dbEntity = enty.getAnnotation(DBEntity.class);
//                String tableName = dbEntity.tableName();
//                if (dbEntity.tableNum() > 1) {
//                    tableName += "_1";
//                }
//                try {
//                    dataAccessor.queryMap(String.format("select * from %s limit 1", tableName));
//                } catch (Exception e) {
//                    if (ExceptionUtils.getRootCauseMessage(e).indexOf("doesn't exist") > -1) {
//                        try {
//                            String createSql = FileUtils.readFileToString(new File("./others/database", dbEntity.tableName() + ".sql"), "UTF-8");
//                            if (createSql.length() > 10) {
//                                for(String sql:createSql.split(";")) {
//                                    // 只执行 CREATE TABLE 语句
//                                    if(sql.length() < 10 || sql.toLowerCase().indexOf("create table") == -1 || sql.indexOf("--") > -1 || sql.indexOf("/*") > -1 || sql.toLowerCase().indexOf("drop table") > -1) {
//                                        continue;
//                                    }
//                                    dataAccessor.updateForCloseConn(sql);
//                                    System.err.println(sql);
//                                }
//                            }
//                        } catch (IOException e1) {
//                            throw new MMException(e);
//                        }
//                    }
//                }
//            }
//        }
    }

    public CacheEntity getCacheEntity(String key){
        Map<String,CacheEntity> map = cacheEntitys.get();
        if(map != null){
            return map.get(key);
        }
        return null;
    }

    public void clearCacheEntitys(){
        cacheEntitys.remove();
    }

    public <T> T selectCreateIfAbsent(Class<T> entityClass,EntityCreator entityCreator, String condition, Object... params){
        T result = selectObject(entityClass,condition,params);
        if(result == null){
            try {
                result = entityClass.newInstance();
                entityCreator.create(result);
                insert(result);
            }catch (Throwable e){
                throw new MMException(e);
            }
        }
        return result;
    }
    /**
     * 查询一个对象，condition必须是主键，否则请用selectList
     *
     * 这里需要保存一下获取数据的版本，其实就是保存一下CacheEntity的引用，这样可以在update的时候cas用
     */
    public <T> T selectObject(Class<T> entityClass, String condition, Object... params){
        // 如果在事务中，先从事务缓存中get，如果事务缓存中没有，则从数据中取，并放入事务,根据情况确定是否返回
        String key = KeyParser.parseKeyForObject(entityClass,condition,params);
        Object object = null;
        if(txCacheService.isInTx()){
            TxCacheService.PrepareCachedData prepareCachedData = txCacheService.get(key);
            if(prepareCachedData != null){
                monitorService.addMonitorNum(MonitorNumType.TxCacheHitNum,1);
                if(prepareCachedData.getOperType() != OperType.Delete){
                    return (T)prepareCachedData.getData();
                }else{ // 是delete说明已经被删除了
                    return null;
                }
            }
        }
        // 如果不在事务之中
        CacheEntity entity = (CacheEntity)cacheService.get(key);
        if(entity == null){
            object = DataSet.select(entityClass,condition,params);
            monitorService.addMonitorNum(MonitorNumType.SelectSqlNum,1);
            if(object != null){
                entity = new CacheEntity(object);
                cacheService.putIfAbsent(key,entity);
            }else{
                // 这里缓存有两种设计方案，一个是不缓存，一个是缓存一个无效值，防止总是通过查询为空来判断某一个条件，导致不断穿透到数据库
                entity = new CacheEntity(object);
                entity.setState(CacheEntity.CacheEntityState.HasNot);
                cacheService.putIfAbsent(key,entity);
            }
        }else{
            monitorService.addMonitorNum(MonitorNumType.CacheHitNum,1);
        }
        if(entity!= null && entity.getState() == CacheEntity.CacheEntityState.Normal) {
            if(txCacheService.isInTx()){
                // 放入事务缓存，这里可以考虑不放入，下面的selectList也就可以不放入
                txCacheService.putWhileSelect(key,entity.getEntity());
                // 如果这个对象要求加锁,那么就要记录下来它的casUnique
                if(txCacheService.isLockClass(entity.getEntity().getClass())){
                    Map<String,CacheEntity> map = cacheEntitys.get();
                    if(map == null){
                        map = new HashMap<>();
                        cacheEntitys.set(map);
                    }
                    map.put(key,entity);
                }
            }
            return (T) (entity.getEntity());
        }
        return null;
    }
    /**
     * 查询一个列表
     * 这里先不做事务的缓存,我觉得还是有必要加的，不过要先想好怎么加，否则会影响效率(和缓存中一样,会有点鸡肋，要不上面的也不缓存)
     */
    public <T> List<T> selectList(Class<T> entityClass, String condition, Object... params) {
        String listKey = KeyParser.parseKeyForList(entityClass,condition,params);

        CacheEntity entity = (CacheEntity)cacheService.get(listKey);
        List<T> objectList = null;
        if(entity == null){
            // TODO 加锁listKey:这里为什么要加锁？
            boolean lock = lockerService.lockKeys(listKey);
            if(!lock){
//                ExceptionHelper.handle();
            }
            try {
                // TODO 这里从异步数据获取满足条件(listKey)的数据，并在查询数据库之后放进对应的list中
                List<AsyncService.AsyncData> asyncDataList = asyncService.getAsyncDataBelongListKey(entityClass,listKey);
                objectList = DataSet.selectListWithCondition(entityClass, condition, params);
                monitorService.addMonitorNum(MonitorNumType.SelectSqlNum,1);
                if (objectList != null || (asyncDataList != null && asyncDataList.size() > 0)) { // 0个也缓存
                    if (objectList == null) {
                        objectList = new ArrayList<>();
                    }
                    if (asyncDataList != null && asyncDataList.size() > 0) {
                        // 放入objcetList
                        Set<String> deleteObjecKeys = null;
                        Set<String> objectListKeys = null;
                        for (AsyncService.AsyncData asyncData : asyncDataList) {
                            if (asyncData.getOperType() == OperType.Insert) {
                                // TODO 为了防止重复数据，这样去除效率有点低，能通过其它方法提高吗，尽管发生的概率比较小，下面删除能否利用上?
                                if (objectListKeys == null) {
                                    objectListKeys = new HashSet<>(objectList.size());
                                    for (Object object : objectList) {
                                        objectListKeys.add(KeyParser.parseKey(object));
                                    }
                                }
                                if (!objectListKeys.contains(KeyParser.parseKey(asyncData.getObject()))) {
                                    objectList.add((T) asyncData.getObject());
                                }
                            } else if (asyncData.getOperType() == OperType.Delete) {
                                if (deleteObjecKeys == null) {
                                    deleteObjecKeys = new HashSet<>();
                                }
                                deleteObjecKeys.add(asyncData.getKey());
                            }
                        }
                        if (deleteObjecKeys != null) {
                            int count = deleteObjecKeys.size();
                            if (count > 0) {
                                int delCount = 0;
                                for (Iterator<T> iter = objectList.iterator(); iter.hasNext(); ) { // 遍历一遍即可删除
                                    if (deleteObjecKeys.contains(KeyParser.parseKey(iter.next()))) {
                                        iter.remove();
                                        if (++delCount >= count) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 缓存两步,一步缓存keys,一步缓存内容
                    LinkedHashSet<String> keys = new LinkedHashSet<>();
                    Map<String, CacheEntity> cacheEntityMap = new HashMap<>();
                    for (T t : objectList) {
                        String key = KeyParser.parseKey(t);
                        CacheEntity cacheEntity = new CacheEntity(t);
                        cacheEntityMap.put(key, cacheEntity);
                        keys.add(key);
                    }
                    // 缓存keys
                    entity = new CacheEntity(keys);
                    cacheService.putIfAbsent(listKey, entity);
                    // 缓存内容
                    if (cacheEntityMap.size() > 0) {
                        cacheService.putList(cacheEntityMap);
                        // 如果这个对象要求加锁,那么就要记录下来它的casUnique
                        boolean tx = txCacheService.isInTx();
                        if(tx){
                            Map<String,CacheEntity> map = cacheEntitys.get();
                            if(map == null){
                                map = new HashMap<>();
                                cacheEntitys.set(map);
                            }
                            if(txCacheService.isLockClass(entityClass)){
                                for(Map.Entry<String,CacheEntity> entry : cacheEntityMap.entrySet()){
                                    map.put(entry.getKey(),entry.getValue());
                                }
                            }
//                            for(Map.Entry<String,CacheEntity> entry : cacheEntityMap.entrySet()){
//                                if(txCacheService.isLockClass(entry.getValue().getEntity().getClass())){
//                                    map.put(entry.getKey(),entry.getValue());
//                                }
//                            }
                        }
                    }
                }
            }finally {
                if(lock) {
                    // TODO 解锁listKey
                    lockerService.unlockKeys(listKey);
                }
            }
        }else{
            monitorService.addMonitorNum(MonitorNumType.CacheHitNum,1);
        }
        LinkedHashSet<String> keys = null;
        if(objectList == null && entity != null){ // 从缓存中取出了对应的keys,需要从缓存中取出指
            keys = (LinkedHashSet<String>) entity.getEntity();
            if(keys.size()>0) {
                List<CacheEntity> cacheEntitieList = cacheService.getList(keys.toArray(new String[keys.size()]));
                // 这里是否需要筛选掉无效的?是不需要的,因为在其它线程置无效标志的时候,就将相应的列表删除完了
                objectList = new ArrayList<T>();
                if(cacheEntitieList != null && cacheEntitieList.size()>0) {
                    for (CacheEntity cacheEntity : cacheEntitieList) {
                        objectList.add((T) cacheEntity.getEntity());
                    }
                    // 如果这个对象要求加锁,那么就要记录下来它的casUnique
                    if(txCacheService.isInTx()){
                        Map<String,CacheEntity> map = cacheEntitys.get();
                        if(map == null){
                            map = new HashMap<>();
                            cacheEntitys.set(map);
                        }
                        int index = 0;
                        boolean isLockClass = txCacheService.isLockClass(entityClass);
                        if(isLockClass){
                            for(String key: keys){
                                map.put(key,cacheEntitieList.get(index));
                                index++;
                            }
                        }
                    }
                }else{
                    // TODO 取出多个值,如果有不存在的还要去数据库中取
                    logger.error("if log here ,call zhengyuzhen ,listkey={},keys={}",listKey,keys);
                }
            }
        }
        if(txCacheService.isInTx() && objectList != null){
            // 替换掉事务中新的值,增删改
            txCacheService.replaceCacheObjectToList(listKey,objectList,keys);
        }
        if(objectList == null){
            return new ArrayList<>();
        }
        return objectList;
    }

    /**
     * 插入一个对象，
     */
    public boolean insert(Object object){
        return insert(object,true);
    }
    public boolean insert(Object object,boolean async){
        String key = KeyParser.parseKey(object);
        // 在事事务中仅插入事务
        if(txCacheService.isInTx()){
            txCacheService.insert(key,object);
            return true;
        }
        // 不在事务中,先插入缓存,再插入异步服务器
        // 这里要用update,因为delete用的是异步,缓存中存在其key,insert不考虑其版本
        // 不会出现同时插入一个object的现象,因为1不可能出现同样的id,2,要加锁
        CacheEntity cacheEntity = new CacheEntity(object);
        cacheService.update(key,cacheEntity);
        // 异步
        if(async) {
            asyncService.insert(key, object);
        }
        return true;
    }
    /**
     * 更新一个对象，
     * 分为两种情况:需要cas和不需要cas
     * 加锁更新的就用cas,否则就不用cas
     */
    public Object update(Object object){
        return update(object,true);
    }
    public Object update(Object object,boolean async){
        String key = KeyParser.parseKey(object);
        // 在事事务中仅更新事务
        if(txCacheService.isInTx()){
            return txCacheService.update(key,object);
        }
        // 不再事务中,先更新缓存,再放入异步数据库
        Map<String,CacheEntity> cacheEntityMap = cacheEntitys.get();
        CacheEntity cacheEntity = null;
        if(cacheEntityMap != null){ // 说明本线程在之前存在加锁的类
            cacheEntity = cacheEntityMap.get(key);
        }
        if(cacheEntity == null){ // 说明本对象不需要版本校验
            cacheEntity = new CacheEntity(object);
        }else{
            // 版本+1
            cacheEntity.setCasUnique(cacheEntity.getCasUnique()+1);
        }
        cacheEntity.setEntity(object);
        cacheEntity.setState(CacheEntity.CacheEntityState.Normal);
        Object old = cacheService.update(key,cacheEntity); // 没有cas,也就可以没有失败
        // 异步
        if(async) {
            asyncService.update(key, object);
        }
        return old;
    }
    /**
     * 删除一个实体
     * 由于要异步删除，缓存中设置删除标志位,所以，在缓存中是update
     */
    public boolean delete(Object object){
        return delete(object,true);
    }
    public boolean delete(Object object,boolean async){
        String key = KeyParser.parseKey(object);
        // 在事事务中仅在事务中删除事务
        if(txCacheService.isInTx()){
            txCacheService.delete(key,object);
            return true;
        }
        // 不再事务中,先更新缓存,再放入异步数据库
        CacheEntity cacheEntity = new CacheEntity(object);
        cacheEntity.setEntity(object);
        cacheEntity.setState(CacheEntity.CacheEntityState.Delete);
        cacheService.update(key,cacheEntity); // 这里用update
        // 异步
        if(async) {
            asyncService.delete(key, object);
        }
        return true;
    }
    // TODO 按照条件删除对象，可以优化
    public <T> boolean delete(Class<T> entityClass, String condition, Object... params){
        T t = selectObject(entityClass,condition,params);
        if(t != null){
            return delete(t);
        }
        return true;
    }


    ///////////////-------------------下面是直接操作数据库的-----------------------

    public long selectCount(Class<?> entityClass, String condition, Object... params){
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        return DataSet.selectCount(entityClass,condition,params);
    }
    public long selectCountBySql(String sql, Object... params){
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        return DatabaseHelper.queryCount(sql, params);
    }

    //
    public <T> List<T> selectListBySql(Class<T> entityClass, String sql, Object... params) {
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        List<T> objectList = null;
        objectList = DatabaseHelper.queryEntityList(entityClass, sql, params);
        return objectList;
    }
    public <T> T selectObjectBySql(Class<T> entityClass, String sql, Object... params) {
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        return DatabaseHelper.queryEntity(entityClass,sql,params);
    }

    /**
     * 执行更新语句（包括：update、insert、delete）
     */
    public void executeBySql(String sql, Object... params){
        monitorService.addMonitorNum(MonitorNumType.ExecuteSqlNum,1);
        DatabaseHelper.updateForCloseConn(sql, params);
    }


    /**
     * 删除一个实体,condition必须是主键
     *
     * 暂时先不用
     */
//    public <T> boolean delete(Class<T> entityClass, String condition, Object... params){
//        return false;
//    }

//    public static void main(String[] args){
//        List<Object> list = new ArrayList<>();
//        list.add(new Object());
//        list.add(null);
//        list.add(null);
//        list.add(new CacheEntity(null));
//        for (Object object :
//                list) {
//        }
//    }
}
