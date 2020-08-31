package com.peony.core.data.persistence.ds.impl;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * 默认数据源工厂
 * <br/>
 * 基于 Apache Commons DBCP 实现
 *
 * @author huangyong
 * @since 2.3
 */
public class HikariDataSourceFactory extends AbstractDataSourceFactory<DataSource> {
    volatile HikariDataSource basicDataSource = null;

    @Override
    public final DataSource getDataSource() {
        if (basicDataSource == null) {
            HikariDataSource ds = (HikariDataSource) create();

            basicDataSource = ds;
        }
        return basicDataSource;
    }

    private DataSource create() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driver);

        ds.setReadOnly(false);
        // 等待连接池分配连接的最大时长（毫秒），超过这个时长还没可用的连接则发生SQLException， 缺省:30秒
        ds.setConnectionTimeout(30000);
        // 一个连接idle状态的最大时长（毫秒），超时则被释放（retired），缺省:10分钟
//        config.setIdleTimeout(configEntity.getMaxIdleTime());
        // 一个连接的生命时长（毫秒），超时而且没被使用则被释放（retired），缺省:30分钟，建议设置比数据库超时时长少30秒，参考MySQLwait_timeout参数（show variables like '%timeout%';）
        ds.setMaxLifetime(1800000 - 60000);
        // 连接池中允许的最大连接数。缺省值：10；推荐的公式：((core_count * 2) + effective_spindle_count)
        ds.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 5 + 30);
        ds.setConnectionInitSql("set names utf8mb4"); // 支持表情符
//        ds.setInitialSize(1);
//        config.setMinIdle(1);
//        ds.setMaxActive(400);
//        ds.setMaxWait(10000);
//        ds.setTimeBetweenEvictionRunsMillis(600000);
//        ds.setMinEvictableIdleTimeMillis(600000);
//        ds.setTestWhileIdle(true);
//        ds.setTestOnBorrow(true);
//        ds.setTestOnReturn(false);
//        ds.setValidationQuery("select 1");
//        ds.setConnectionInitSqls(Arrays.asList("set names utf8mb4")); // 支持表情符

//        ds.addDataSourceProperty("cachePrepStmts", "true");
//        ds.addDataSourceProperty("prepStmtCacheSize", "250");
//        ds.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return ds;
    }

    @Override
    public DataSource createDataSource() {
        return create();
    }

    @Override
    public void setDriver(DataSource ds, String driver) {
        basicDataSource.setDriverClassName(driver);
    }

    @Override
    public void setUrl(DataSource ds, String url) {
        basicDataSource.setJdbcUrl(url);
    }

    @Override
    public void setUsername(DataSource ds, String username) {
        basicDataSource.setUsername(username);
    }

    @Override
    public void setPassword(DataSource ds, String password) {
        basicDataSource.setPassword(password);
    }

    @Override
    public void setAdvancedConfig(DataSource ds) {
        /**
         * <!-- 配置初始化大小、最小、最大 -->
         <property name="initialSize" value="1" />
         <property name="minIdle" value="1" />
         <property name="maxActive" value="100" />

         <!-- 配置获取连接等待超时的时间 -->
         <property name="maxWait" value="10000" />

         <!-- 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒 -->
         <property name="timeBetweenEvictionRunsMillis" value="600000" />

         <!-- 配置一个连接在池中最小生存的时间，单位是毫秒 -->
         <property name="minEvictableIdleTimeMillis" value="600000" />

         <property name="testWhileIdle" value="true" />

         <!-- 这里建议配置为TRUE，防止取到的连接不可用 -->
         <property name="testOnBorrow" value="false" />
         <property name="testOnReturn" value="false" />

         <!-- 验证连接有效与否的SQL，不同的数据配置不同 -->
         <property name="validationQuery" value="select 1 " />
         */
//        ds.setInitialSize(1);
//        ds.setMinIdle(1);
//        ds.setMaxActive(400);
//        ds.setMaxWait(10000);
//        ds.setTimeBetweenEvictionRunsMillis(600000);
//        ds.setMinEvictableIdleTimeMillis(600000);
//        ds.setTestWhileIdle(true);
//        ds.setTestOnBorrow(true);
//        ds.setTestOnReturn(false);
//        ds.setValidationQuery("select 1");
//        ds.setConnectionInitSqls(Arrays.asList("set names utf8mb4")); // 支持表情符
        // 解决 java.sql.SQLException: Already closed. 的问题（连接池会自动关闭长时间没有使用的连接）
//        ds.setValidationQuery("select 1 from dual");

        //   basicDataSource.setConnectionInitSqls(Arrays.asList("set names utf8mb4")); // 支持表情符

    }
}
