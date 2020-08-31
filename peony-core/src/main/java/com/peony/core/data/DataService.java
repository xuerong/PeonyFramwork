package com.peony.core.data;

import com.peony.core.control.annotation.Service;
import com.peony.core.data.persistence.dao.DatabaseHelper;
import com.peony.core.data.persistence.orm.DataSet;
import com.peony.core.data.persistence.orm.annotation.DBEntity;
import com.peony.core.data.tx.Tx;
import com.peony.core.security.MonitorNumType;
import com.peony.core.security.MonitorService;
import com.peony.common.exception.MMException;
import com.peony.common.tool.helper.ClassHelper;
import com.peony.core.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * 数据服务。
 * <p>
 * <strong>这个是操作数据的对外接口，也是唯一接口。提供了对数据的增删改查等功能，针对的
 * 对象是DBEntity对象，不要该数据服务之外的工具进行数据操作，除非你对数据操作服务有充分
 * 的了解</strong>
 * <p>
 * DataService对数据的操作主要包括两种类型：一种是对数据的基本操作，无需自己写sql语句；
 * 另一种是通过自定义sql来完成操作(方法名以BySql结尾)。除此之外还有一些辅助类型的查询，
 * 是第一种查询的扩展。
 * <p>
 * 第一种包括：
 * <table>
 *     <tr><td>selectObject</td><td>查询单条数据，并转化为java对象</td></tr>
 *     <tr><td>selectList</td><td>查询多条数据，并转化为java对象</td></tr>
 *     <tr><td>insert</td><td>插入一条数据（java对象）</td></tr>
 *     <tr><td>update</td><td>修改一条数据（java对象）</td></tr>
 *     <tr><td>delete</td><td>删除一条数据（java对象）</td></tr>
 * </table>
 * 第二种包括：
 * <table>
 *     <tr><td>selectObjectBySql</td><td>查询单条数据，并转化为java对象</td></tr>
 *     <tr><td>selectListBySql</td><td>查询多条数据，并转化为java对象</td></tr>
 *     <tr><td>selectObjectListBySql</td><td>查询多条数据，可根据需要返回相应字段</td></tr>
 *     <tr><td>executeBySql</td><td>执行更新语句</td></tr>
 * </table>
 * <p>
 * 第一种操作简单方便，并且自动使用框架提供的事务、缓存、并发一致性、异步存储等问题，属于
 * 主要数据操作；第二种将用自定义sql直接操作数据库，主要用于复杂的sql操作，这种操作需要
 * 使用者自己确保并发一致性，缓存等问题，属于辅助数据操作。
 * <p>
 * 需要注意的是：对于第二种操作中的查询相关的操作，由于数据库的更新是异步进行的，所以得到
 * 的数据可能是过期了的；而对于更新相关的操作，由于直接更新数据库不会更新缓存，所以要确保
 * 相关数据没有被第一种操作方式操作。所以，如非必要，请使用第一种操作，否则，尽量避免使用
 * 第二种操作中的更新操作！
 *
 * @author zhengyuzhen
 * @see TxDataInnerService
 * @see DBEntity
 * @since 1.0
 */
@Service(init = "init", initPriority = 2)
public class DataService {
    private static Logger logger = LoggerFactory.getLogger(DataService.class);

    private MonitorService monitorService;
    /**
     * 支持事务的数据服务
     */
    private TxDataInnerService txDataInnerService;


    /**
     * 初始化的时候，检查DBEntity是否
     */
    public void init() {
        List<Class<?>> entities = ClassHelper.getClassListByAnnotation(DBEntity.class,
                Server.getEngineConfigure().getAllPackets());
        for (Class<?> enty : entities) {
            if (!Serializable.class.isAssignableFrom(enty)) {
                throw new MMException(MMException.ExceptionType.StartUpFail,"DBEntity 类型必须实现 Serializable 接口! " + enty.getSimpleName());
            }
        }
    }

    /**
     * 查询，如果没有则创建。
     * <p>
     * <strong>查询条件中，字段的限定只支持 "=" ，多字段的组合只支持 "and"</strong>
     *
     * @param entityClass 查询对象的类型，必须为DBEntity对象
     * @param entityCreator 初始化工具，用于初始化对象数据
     * @param condition 查询条件
     * @param params 条件参数
     * @param <T> 对象类型
     * @return
     */
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
     * 查询一个对象，并转化为对象返回。condition中必须是主键，否则会认为是不止一个对象，
     * 请用selectList
     * <p>
     * <strong>查询条件中，字段的限定只支持 "=" ，多字段的组合只支持 "and"</strong>
     *
     * @param entityClass 对象类型，必须为DBEntity对象
     * @param condition 查询条件
     * @param params 条件参数
     * @param <T> 对象的类型
     * @return 返回查询到的对象，如果没有，则为null
     */
    public <T> T selectObject(Class<T> entityClass, String condition, Object... params){
        return txDataInnerService.selectObject(entityClass, condition, params);
    }

    /**
     * 查询一个数据列表，并转化为对象列表返回。
     * <p>
     * <strong>查询条件中，字段的限定只支持 "=" ，多字段的组合只支持 "and"</strong>
     *
     * @param entityClass 对象类型，必须为DBEntity对象
     * @param condition 查询条件
     * @param params 条件参数
     * @param <T> 对象的类型
     * @return 返回查询到的对象列表，如果没有，则为空的List
     */
    public <T> List<T> selectList(Class<T> entityClass, String condition, Object... params) {
        return txDataInnerService.selectList(entityClass, condition, params);
    }

    /**
     * 插入一个对象。
     *
     * @param object 插入的对象，必须为DBEntity对象
     */
    public void insert(Object object){
        txDataInnerService.insert(object,true);
    }

    /**
     * 更新一个对象。
     *
     * @param object 更新的对象，必须为DBEntity对象
     * @return 旧的对象
     */
    public Object update(Object object){
        return txDataInnerService.update(object,true);
    }

    /**
     * 删除一个对象。
     *
     * @param object 删除的对象，必须为DBEntity对象
     */
    public void delete(Object object){
        txDataInnerService.delete(object,true);
    }

    /**
     * 按照条件删除对象。TODO 可以优化
     *
     * @param entityClass 对象类型，必须为DBEntity对象
     * @param condition 查询条件
     * @param params 条件参数
     * @param <T> 对象的类型
     */
    public <T> void delete(Class<T> entityClass, String condition, Object... params){
        T t = selectObject(entityClass,condition,params);
        if(t != null){
            delete(t);
        }
    }

    /****************************************************************************************************
     *                       下面是直接操作数据库的，不支持事务机制，不能确保数据是最新的                        *
     ****************************************************************************************************/

    /**
     * 查询数量。
     *
     * @param entityClass 对象类型，必须为DBEntity对象
     * @param condition 查询条件
     * @param params 条件参数
     * @return 数量
     */
    public long selectCount(Class<?> entityClass, String condition, Object... params){
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        return DataSet.selectCount(entityClass,condition,params);
    }

    /**
     * 自定义sql查询数量，其中数量字段必须为"count(*)"
     *
     * @param sql 查询用的sql
     * @param params sql中的参数
     * @return 数量
     */
    public long selectCountBySql(String sql, Object... params){
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        return DatabaseHelper.queryCount(sql, params);
    }

    /**
     * 自定义sql查询数据列表，并转化为对象列表。
     * <p>
     * <strong>不支持事务机制，不能确保数据是最新的</strong>
     *
     * @param entityClass 对象类型，必须为DBEntity对象
     * @param sql 查询的sql
     * @param params sql中的参数
     * @param <T> 对象的类型
     * @return 对象列表
     */
    public <T> List<T> selectListBySql(Class<T> entityClass, String sql, Object... params) {
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        List<T> objectList = null;
        objectList = DatabaseHelper.queryEntityList(entityClass, sql, params);
        return objectList;
    }

    /**
     * 自定义sql查询数据，并转化为对象。
     * <p>
     * <strong>不支持事务机制，不能确保数据是最新的</strong>
     *
     * @param entityClass 对象类型，必须为DBEntity对象
     * @param sql 查询的sql
     * @param params sql中的参数
     * @param <T> 对象的类型
     * @return 返回的对象
     */
    public <T> T selectObjectBySql(Class<T> entityClass, String sql, Object... params) {
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum,1);
        return DatabaseHelper.queryEntity(entityClass,sql,params);
    }

    /**
     * 执行更新语句（包括：update、insert、delete）
     * <p>
     * <strong>不支持事务机制，不能确保数据是最新的</strong>
     *
     * @param sql 执行的sql
     * @param params sql中的参数
     */
    public void executeBySql(String sql, Object... params){
        monitorService.addMonitorNum(MonitorNumType.ExecuteSqlNum,1);
        DatabaseHelper.updateForCloseConn(sql, params);
    }

    /**
     * 查询多条数据，可根据需要返回相应字段
     * <p>
     * <strong>不能确保数据是最新的</strong>
     *
     * @param sql 执行的sql
     * @param params sql中的参数
     * @return 返回查询的结果列表
     */
    public List<Object[]> selectObjectListBySql(String sql, Object... params) {
        monitorService.addMonitorNum(MonitorNumType.ExecuteSelectSqlNum, 1);
        return DatabaseHelper.queryArrayList(sql, params);
    }
}
