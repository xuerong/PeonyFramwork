package com.peony.core.data.persistence.ds;

import javax.sql.DataSource;

/**
 * 数据源工厂
 *
 * @author huangyong
 * @since 2.3
 */
public interface DataSourceFactory {

    /**
     * 获取数据源
     *
     * @return 数据源
     */
    DataSource getDataSource();
}
