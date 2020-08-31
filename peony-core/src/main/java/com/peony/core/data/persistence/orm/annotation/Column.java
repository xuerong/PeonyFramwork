package com.peony.core.data.persistence.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义列名
 *
 * @author huangyong
 * @since 1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    String name() default ""; // 默认使用字段名

    StringTypeCollation stringColumnType() default StringTypeCollation.Varchar128; // 这个只有在列的类型是String时才生效，用于决定String在数据库中类型
}
