package com.peony.engine.config.core.field.impl;

import com.peony.engine.config.core.field.SingleField;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class BoolField extends SingleField<Boolean> {

    public BoolField() {
        super("bool", "boolean", "false");
    }

    @Override
    public Boolean parseValue(String rawVal) {
        if (rawVal == null) {
            return false;
        }
        return Boolean.parseBoolean(rawVal.trim());
    }
}
