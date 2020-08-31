package com.peony.demo.config.core.field.impl;

import com.peony.demo.config.core.field.SingleField;
import com.peony.demo.config.core.SplitUtil;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class IntField extends SingleField<Integer> {

    public IntField() {
        super("int", "Integer", "null");
    }

    @Override
    public Integer parseValue(String rawVal) {
        if (rawVal == null || rawVal.equals("")) {
            return 0;
        }
        return SplitUtil.convert(Integer.class, rawVal);
    }
}
