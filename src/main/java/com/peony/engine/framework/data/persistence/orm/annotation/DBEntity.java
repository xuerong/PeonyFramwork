package com.peony.engine.framework.data.persistence.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义 DBEntity 类
 *
 * 所有的数据库对象都要添加该注解，如果需要缓存则
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DBEntity {
    String tableName(); // 表明
    String[] pks(); // 主键，这个是需要的，至少要有一个id作为主键

    /**
     * 分表的表：
     * 1\必须有主键，且分表依据为第一个主键
     * 2\查询的条件，第一个条件必须是分表的主键
     *
     * @return
     */
    int tableNum() default 1; // 表的数量，分表:

    String createSql() default "";
}
