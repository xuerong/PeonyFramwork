package com.peony.engine.config.core.field.impl;

import com.peony.engine.config.core.field.SingleField;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class StringField extends SingleField<String> {

    public StringField() {
        super("string", "String", "null");
    }

    @Override
    public String parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return rawVal.trim();
    }
}
