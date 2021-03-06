package com.peony.core.data.persistence.ds.impl;

import com.peony.core.data.persistence.ds.DataSourceFactory;
import com.peony.core.server.Server;

import javax.sql.DataSource;

/**
 * 抽象数据源工厂
 *
 * @author huangyong
 * @since 2.3
 */
public abstract class AbstractDataSourceFactory<T extends DataSource> implements DataSourceFactory {

    protected final String driver = Server.getEngineConfigure().getString("jdbc.driver");
    protected final String url = Server.getEngineConfigure().getString("jdbc.url");
    protected final String username = Server.getEngineConfigure().getString("jdbc.username");
    protected final String password = Server.getEngineConfigure().getString("jdbc.password");

    @Override
    public T getDataSource() {
        // 创建数据源对象
        T ds = createDataSource();
        // 设置基础属性
        setDriver(ds, driver);
        setUrl(ds, url);
        setUsername(ds, username);
        setPassword(ds, password);
        // 设置高级属性
        setAdvancedConfig(ds);

        return ds;
    }

    public abstract T createDataSource();

    public abstract void setDriver(T ds, String driver);

    public abstract void setUrl(T ds, String url);

    public abstract void setUsername(T ds, String username);

    public abstract void setPassword(T ds, String password);

    public abstract void setAdvancedConfig(T ds);
}
