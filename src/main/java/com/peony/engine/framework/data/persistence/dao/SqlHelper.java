package com.peony.engine.framework.data.persistence.dao;

import com.peony.engine.framework.data.persistence.orm.EntityHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * 封装 SQL 语句相关操作
 *
 * @author huangyong
 * @since 1.0
 */
public class SqlHelper {

    /**
     * SQL 属性文件对象
     */
//    private static final Properties sqlProps = PropsUtil.loadProps(FrameworkConstant.SQL_PROPS);

    /**
     * 从 SQL 属性文件中获取相应的 SQL 语句
     */
//    public static String getSql(String key) {
//        String sql;
//        if (sqlProps.containsKey(key)) {
//            sql = sqlProps.getProperty(key);
//        } else {
//            throw new RuntimeException("无法在 " + FrameworkConstant.SQL_PROPS + " 文件中获取属性：" + key);
//        }
//        return sql;
//    }

    /**
     * 生成 select 语句
     */
    public static String generateSelectSql(Class<?> entityClass, String condition, String sort,int id) {
        StringBuilder sql = new StringBuilder("select * from ").append(getTable(entityClass,id));
        sql.append(generateWhere(condition));
        sql.append(generateOrder(sort));
        return sql.toString();
    }

    /**
     * 生成 insert 语句
     */
    public static String generateInsertSql(Class<?> entityClass, Collection<String> fieldNames,int id) {
        StringBuilder sql = new StringBuilder("insert into ").append(getTable(entityClass,id));
        if (CollectionUtils.isNotEmpty(fieldNames)) {
            int i = 0;
            StringBuilder columns = new StringBuilder(" ");
            StringBuilder values = new StringBuilder(" values ");
            for (String fieldName : fieldNames) {
                String columnName = EntityHelper.getColumnName(entityClass, fieldName);
                if (i == 0) {
                    columns.append("(`").append(columnName);
                    values.append("(?");
                } else {
                    columns.append("`, `").append(columnName);
                    values.append(", ?");
                }
                if (i == fieldNames.size() - 1) {
                    columns.append("`)");
                    values.append(")");
                }
                i++;
            }
            sql.append(columns).append(values);
        }
        return sql.toString();
    }

    /**
     * 生成 delete 语句
     */
    public static String generateDeleteSql(Class<?> entityClass, String condition,int id) {
        StringBuilder sql = new StringBuilder("delete from ").append(getTable(entityClass,id));
        sql.append(generateWhere(condition));
        return sql.toString();
    }

    /**
     * 生成 update 语句
     */
    public static String generateUpdateSql(Class<?> entityClass, Map<String, Object> fieldMap, String condition,int id) {
        StringBuilder sql = new StringBuilder("update ").append(getTable(entityClass,id));
        if (MapUtils.isNotEmpty(fieldMap)) {
            sql.append(" set ");
            int i = 0;
            for (Map.Entry<String, Object> fieldEntry : fieldMap.entrySet()) {
                String fieldName = fieldEntry.getKey();
                String columnName = EntityHelper.getColumnName(entityClass, fieldName);
                if (i == 0) {
                    sql.append("`").append(columnName).append("` = ?");
                } else {
                    sql.append(", `").append(columnName).append("` = ?");
                }
                i++;
            }
        }
        sql.append(generateWhere(condition));
        return sql.toString();
    }

    /**
     * 生成 select count(*) 语句
     */
    public static String generateSelectSqlForCount(Class<?> entityClass, String condition) {
        StringBuilder sql = new StringBuilder("select count(*) from ").append(getTable(entityClass,0));
        sql.append(generateWhere(condition));
        return sql.toString();
    }

    /**
     * 生成 select 分页语句（数据库类型为：mysql、oracle、mssql）
     */
    public static String generateSelectSqlForPager(int pageNumber, int pageSize, Class<?> entityClass, String condition, String sort,int id) {
        StringBuilder sql = new StringBuilder();
        String table = getTable(entityClass,id);
        String where = generateWhere(condition);
        String order = generateOrder(sort);
        String dbType = DatabaseHelper.getDatabaseType();
        if (dbType.equalsIgnoreCase("mysql")) {
            int pageStart = (pageNumber - 1) * pageSize;
            appendSqlForMySql(sql, table, where, order, pageStart, pageSize);
        } else if (dbType.equalsIgnoreCase("oracle")) {
            int pageStart = (pageNumber - 1) * pageSize + 1;
            int pageEnd = pageStart + pageSize;
            appendSqlForOracle(sql, table, where, order, pageStart, pageEnd);
        } else if (dbType.equalsIgnoreCase("mssql")) {
            int pageStart = (pageNumber - 1) * pageSize;
            appendSqlForMsSql(sql, table, where, order, pageStart, pageSize);
        }
        return sql.toString();
    }

    private static String getTable(Class<?> entityClass,int id) {
        return EntityHelper.getTableName(entityClass,id);
    }

    private static String generateWhere(String condition) {
        String where = "";
        if (StringUtils.isNotEmpty(condition)) {
            where += " where " + condition;
        }
        return where;
    }

    private static String generateOrder(String sort) {
        String order = "";
        if (StringUtils.isNotEmpty(sort)) {
            order += " order by " + sort;
        }
        return order;
    }

    private static void appendSqlForMySql(StringBuilder sql, String table, String where, String order, int pageStart, int pageEnd) {
        /*
            select * from 表名 where 条件 order by 排序 limit 开始位置, 结束位置
         */
        sql.append("select * from ").append(table);
        sql.append(where);
        sql.append(order);
        sql.append(" limit ").append(pageStart).append(", ").append(pageEnd);
    }

    private static void appendSqlForOracle(StringBuilder sql, String table, String where, String order, int pageStart, int pageEnd) {
        /*
            select a.* from (
                select rownum rn, t.* from 表名 t where 条件 order by 排序
            ) a
            where a.rn >= 开始位置 and a.rn < 结束位置
        */
        sql.append("select a.* from (select rownum rn, t.* from ").append(table).append(" t");
        sql.append(where);
        sql.append(order);
        sql.append(") a where a.rn >= ").append(pageStart).append(" and a.rn < ").append(pageEnd);
    }

    private static void appendSqlForMsSql(StringBuilder sql, String table, String where, String order, int pageStart, int pageEnd) {
        /*
            select top 结束位置 * from 表名 where 条件 and id not in (
                select top 开始位置 id from 表名 where 条件 order by 排序
            ) order by 排序
        */
        sql.append("select top ").append(pageEnd).append(" * from ").append(table);
        if (StringUtils.isNotEmpty(where)) {
            sql.append(where).append(" and ");
        } else {
            sql.append(" where ");
        }
        sql.append("id not in (select top ").append(pageStart).append(" id from ").append(table);
        sql.append(where);
        sql.append(order);
        sql.append(") ").append(order);
    }
}
