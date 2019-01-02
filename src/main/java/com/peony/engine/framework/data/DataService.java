package com.peony.engine.framework.data;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.cache.CacheService;
import com.peony.engine.framework.data.persistence.dao.DatabaseHelper;
import com.peony.engine.framework.data.persistence.orm.DataSet;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.tx.AsyncService;
import com.peony.engine.framework.data.tx.LockerService;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.security.MonitorNumType;
import com.peony.engine.framework.security.MonitorService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.ClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private MonitorService monitorService;
    private TxDataInnerService txDataInnerService;


    public void init() {
        List<Class<?>> entities = ClassHelper.getClassListByAnnotation(DBEntity.class);
        for (Class<?> enty : entities) {
            if (!Serializable.class.isAssignableFrom(enty)) {
                throw new MMException("DBEntity 类型必须实现 Serializable 接口! " + enty.getSimpleName());
            }
        }
    }

    public <T> T selectCreateIfAbsent(Class<T> entityClass,EntityCreator entityCreator, String condition, Object... params){
        T result = selectObject(entityClass,condition,params);
        if(result == null){
            result = createEntity(entityClass,entityCreator,condition,params);
        }
        return result;
    }
    @Tx
    public <T> T createEntity(Class<T> entityClass,EntityCreator entityCreator, String condition, Object... params){
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
        return txDataInnerService.selectObject(entityClass, condition, params);
    }
    /**
     * 查询一个列表
     * 这里先不做事务的缓存,我觉得还是有必要加的，不过要先想好怎么加，否则会影响效率(和缓存中一样,会有点鸡肋，要不上面的也不缓存)
     */
    public <T> List<T> selectList(Class<T> entityClass, String condition, Object... params) {
        return txDataInnerService.selectList(entityClass, condition, params);
    }

    /**
     * 插入一个对象，
     */
    public void insert(Object object){
        txDataInnerService.insert(object,true);
    }
    /**
     * 更新一个对象，
     * 分为两种情况:需要cas和不需要cas
     * 加锁更新的就用cas,否则就不用cas
     */
    public Object update(Object object){
        return txDataInnerService.update(object,true);
    }
    /**
     * 删除一个实体
     * 由于要异步删除，缓存中设置删除标志位,所以，在缓存中是update
     */
    public void delete(Object object){
        txDataInnerService.delete(object,true);
    }

    // TODO 按照条件删除对象，可以优化
    public <T> void delete(Class<T> entityClass, String condition, Object... params){
        T t = selectObject(entityClass,condition,params);
        if(t != null){
            delete(t);
        }
//        return true;
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

    public List<Object[]> selectObjectListBySql(String sql, Object... params) {
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum, 1);
        return DatabaseHelper.queryArrayList(sql, params);
    }
}
