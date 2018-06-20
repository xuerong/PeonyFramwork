package com.peony.engine.config.core.field.impl;

import com.peony.engine.config.core.field.SingleField;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class LongField extends SingleField<Long> {

    public LongField() {
        super("long", "Long", "null");
    }

    @Override
    public Long parseValue(String rawVal) {
        if (rawVal == null || rawVal.equals("")) {
            return 0L;
        }
        return Long.valueOf(rawVal.trim());
    }
}
