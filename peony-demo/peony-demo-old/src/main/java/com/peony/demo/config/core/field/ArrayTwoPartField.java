package com.peony.demo.config.core.field;

import java.util.Collection;

/**
 * 包含两个字段的值类型, 如: array<map/tuple2>
 *
 * <p>
 * Created by jiangmin.wu on 2018/3/6.
 */
public abstract class ArrayTwoPartField<T extends Collection> extends TwoPartField<T> {

    public ArrayTwoPartField(String name, String javaType, String defaultVal, String verifyKey1, String verifyKey2) {
        super(name, javaType, defaultVal, verifyKey1, verifyKey2);
    }
}
