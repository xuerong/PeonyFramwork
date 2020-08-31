package com.peony.core.data.persistence.dao;

import com.peony.core.data.persistence.ds.DataSourceFactory;
import com.peony.common.exception.MMException;
import com.peony.core.server.Server;
import com.peony.core.control.BeanHelper;
import com.peony.common.tool.util.ClassUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 封装数据库相关操作
 *
 * @author huangyong
 * @since 1.0
 */
public class DatabaseHelper {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);

    /**
     * 定义一个局部线程变量（使每个线程都拥有自己的连接）
     */
    private static final ThreadLocal<Connection> connContainer ;

    /**
     * 获取数据访问器
     */
    private static final DataAccessor dataAccessor ;

    /**
     * 数据库类型
     */
    private static final String databaseType ;
    static {
        dataAccessor = BeanHelper.getFrameBean(DataAccessor.class);
        databaseType = Server.getEngineConfigure().getString("jdbc.type");
        connContainer = new ThreadLocal<Connection>();
    }

    /**
     * 获取数据库类型
     */
    public static String getDatabaseType() {
        return databaseType;
    }

    /**
     * 获取数据源
     */
    public static DataSource getDataSource() {
        // 提前初始化会出现热插拔错误，即DataAccessor在初始化自身的时候调用了该方法，而此前若初始化导致获取的DataSourceFactory为空
        return BeanHelper.getFrameBean(DataSourceFactory.class).getDataSource();
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() {
        Connection conn;
        try {
            // 先从 ThreadLocal 中获取 Connection
            conn = connContainer.get();
            if (conn == null) {
                // 若不存在，则从 DataSource 中获取 Connection
                conn = getDataSource().getConnection();
                // 将 Connection 放入 ThreadLocal 中
                if (conn != null) {
                    connContainer.set(conn);
                    if(conn.isClosed()){
                        logger.error("new conn is closed while getConnection");
                        throw new MMException("new conn is closed while getConnection1");
                    }
                }else{
                    logger.error("new conn is null");
                    throw new MMException("new conn is null1");
                }

            }else if(conn.isClosed()){
                // 若不存在，则从 DataSource 中获取 Connection
                conn = getDataSource().getConnection();
                // 将 Connection 放入 ThreadLocal 中
                if (conn != null) {
                    connContainer.set(conn);
                }else{
                    logger.error("new conn is null");
                    throw new MMException("new conn is null2");
                }
                logger.warn("conn is closed while getConnection,and get new from pool"+conn);

                if(conn.isClosed()){
                    logger.error("new conn is closed while getConnection");
                    throw new MMException("new conn is closed while getConnection2");
                }
            }
        } catch (SQLException e) {
            logger.error("获取数据库连接出错！", e);
            throw new RuntimeException(e);
        }
        return conn;
    }

    /**
     * 开启事务
     */
    public static void beginTransaction() {
        Connection conn = getConnection();
        if (conn != null) {
            try {
                conn.setAutoCommit(false);
            } catch (SQLException e) {
                logger.error("开启事务出错！", e);
                throw new RuntimeException(e);
            } finally {
                connContainer.set(conn);
            }
        }
    }

    /**
     * 提交事务
     */
    public static void commitTransaction() {
        Connection conn = getConnection();
        if (conn != null) {
            try {
                conn.commit();
                conn.close();
            } catch (SQLException e) {
                logger.error("提交事务出错！", e);
                throw new RuntimeException(e);
            } finally {
                connContainer.remove();
            }
        }
    }

    /**
     * 回滚事务
     */
    public static void rollbackTransaction() {
        Connection conn = getConnection();
        if (conn != null) {
            try {
                conn.rollback();
                conn.close();
            } catch (SQLException e) {
                logger.error("回滚事务出错！", e);
                throw new RuntimeException(e);
            } finally {
                connContainer.remove();
            }
        }
    }

    /**
     * 初始化 SQL 脚本
     */
    public static void initSQL(String sqlPath) {
        try {
            File sqlFile = new File(ClassUtil.getClassPath() + sqlPath);
            List<String> sqlList = FileUtils.readLines(sqlFile);
            for (String sql : sqlList) {
                updateForCloseConn(sql);
            }
        } catch (Exception e) {
            logger.error("初始化 SQL 脚本出错！", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据 SQL 语句查询 DBEntity
     */
    public static <T> T queryEntity(Class<T> entityClass, String sql, Object... params) {
        return dataAccessor.queryEntity(entityClass, sql, params);
    }

    /**
     * 根据 SQL 语句查询 DBEntity 列表
     */
    public static <T> List<T> queryEntityList(Class<T> entityClass, String sql, Object... params) {
        return dataAccessor.queryEntityList(entityClass, sql, params);
    }

    /**
     * 根据 SQL 语句查询 DBEntity 映射（Field Name => Field Value）
     */
    public static <K, V> Map<K, V> queryEntityMap(Class<V> entityClass, String sql, Object... params) {
        return dataAccessor.queryEntityMap(entityClass, sql, params);
    }

    /**
     * 根据 SQL 语句查询 Array 格式的字段（单条记录）
     */
    public static Object[] queryArray(String sql, Object... params) {
        return dataAccessor.queryArray(sql, params);
    }

    /**
     * 根据 SQL 语句查询 Array 格式的字段列表（多条记录）
     */
    public static List<Object[]> queryArrayList(String sql, Object... params) {
        return dataAccessor.queryArrayList(sql, params);
    }

    /**
     * 根据 SQL 语句查询 Map 格式的字段（单条记录）
     */
    public static Map<String, Object> queryMap(String sql, Object... params) {
        return dataAccessor.queryMap(sql, params);
    }

    /**
     * 根据 SQL 语句查询 Map 格式的字段列表（多条记录）
     */
    public static List<Map<String, Object>> queryMapList(String sql, Object... params) {
        return dataAccessor.queryMapList(sql, params);
    }

    /**
     * 根据 SQL 语句查询指定字段（单条记录）
     */
    public static <T> T queryColumn(String sql, Object... params) {
        return dataAccessor.queryColumn(sql, params);
    }

    /**
     * 根据 SQL 语句查询指定字段列表（多条记录）
     */
    public static <T> List<T> queryColumnList(String sql, Object... params) {
        return dataAccessor.queryColumnList(sql, params);
    }

    /**
     * 根据 SQL 语句查询指定字段映射（多条记录）
     */
    public static <T> Map<T, Map<String, Object>> queryColumnMap(String column, String sql, Object... params) {
        return dataAccessor.queryColumnMap(column, sql, params);
    }

    /**
     * 根据 SQL 语句查询记录条数
     */
    public static long queryCount(String sql, Object... params) {
        return dataAccessor.queryCount(sql, params);
    }

    /**
     * 执行更新语句（包括：update、insert、delete）
     * todo 这个更新语句不能用于普通的sql执行，因为它把连接池放进了ThreadLocal而没有释放，导致泄露,这里只用于已知的异步服务（AsyncService）,后面要优化
     */
    public static int update(String sql, Object... params) {
        return dataAccessor.update(sql, params);
    }

    /**
     * 用于普通sql执行
     * @param sql
     * @param params
     * @return
     */
    public static int updateForCloseConn(String sql, Object... params) {
        return dataAccessor.updateForCloseConn(sql, params);
    }

    /**
     * 执行插入语句，返回插入后的主键
     */
    public static Serializable insertReturnPK(String sql, Object... params) {
        return dataAccessor.insertReturnPK(sql, params);
    }
}
