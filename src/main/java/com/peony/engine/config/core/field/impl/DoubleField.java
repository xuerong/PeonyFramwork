package com.peony.engine.config.core.field.impl;

import com.peony.engine.config.core.field.SingleField;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class DoubleField extends SingleField<Double> {

    public DoubleField() {
        super("double", "Double", "null");
    }

    @Override
    public Double parseValue(String rawVal) {
        if (rawVal == null || rawVal.equals("")) {
            return 0D;
        }
        return Double.valueOf(rawVal.trim());
    }
}
