package com.peony.demo.config.core.field.impl;

import com.peony.demo.config.core.field.SingleField;

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
        rawVal = rawVal.trim();
        return rawVal.equals("1") || Boolean.parseBoolean(rawVal);
    }
}
