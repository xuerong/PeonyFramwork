package com.peony.engine.config.core.field;

import java.util.Collection;

/**
 * 包含两个字段的值类型, 如: array<map/tuple2>
 *
 * <p>
 * Created by jiangmin.wu on 2018/3/6.
 */
public abstract class ArrayThreePartField<T extends Collection> extends ThreePartField<T> {

    public ArrayThreePartField(String name, String javaType, String defaultVal, String verifyKey1, String verifyKey2, String verifyKey3) {
        super(name, javaType, defaultVal, verifyKey1, verifyKey2, verifyKey3);
    }
}
