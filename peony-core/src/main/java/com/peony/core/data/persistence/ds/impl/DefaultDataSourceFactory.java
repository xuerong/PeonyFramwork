package com.peony.core.data.persistence.ds.impl;

import org.apache.commons.dbcp2.BasicDataSource;

import java.util.Arrays;

/**
 * 默认数据源工厂
 * <br/>
 * 基于 Apache Commons DBCP 实现
 *
 * @author huangyong
 * @since 2.3
 */
public class DefaultDataSourceFactory extends AbstractDataSourceFactory<BasicDataSource> {

    volatile BasicDataSource basicDataSource = null;

    @Override
    public final BasicDataSource getDataSource() {
        if(basicDataSource == null){
            // 创建数据源对象
            BasicDataSource ds = createDataSource();
            // 设置基础属性
            setDriver(ds, driver);
            setUrl(ds, url);
            setUsername(ds, username);
            setPassword(ds, password);
            // 设置高级属性
            setAdvancedConfig(ds);
            basicDataSource = ds;
        }


        return basicDataSource;
    }

    @Override
    public BasicDataSource createDataSource() {
        return new BasicDataSource();
    }

    @Override
    public void setDriver(BasicDataSource ds, String driver) {
        ds.setDriverClassName(driver);
    }

    @Override
    public void setUrl(BasicDataSource ds, String url) {
        ds.setUrl(url);
    }

    @Override
    public void setUsername(BasicDataSource ds, String username) {
        ds.setUsername(username);
    }

    @Override
    public void setPassword(BasicDataSource ds, String password) {
        ds.setPassword(password);
    }

    @Override
    public void setAdvancedConfig(BasicDataSource ds) {
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
        ds.setInitialSize(1);
        ds.setMinIdle(1);
        ds.setMaxTotal(400);
        ds.setMaxWaitMillis(10000);
        ds.setTimeBetweenEvictionRunsMillis(600000);
        ds.setMinEvictableIdleTimeMillis(600000);
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(false);
        ds.setValidationQuery("select 1");
        ds.setConnectionInitSqls(Arrays.asList("set names utf8mb4")); // 支持表情符
        // 解决 java.sql.SQLException: Already closed. 的问题（连接池会自动关闭长时间没有使用的连接）
//        ds.setValidationQuery("select 1 from dual");
    }
}
