package com.peony.demo.config.core.field.impl;

import com.peony.demo.config.core.SplitUtil;
import com.peony.demo.config.core.field.SingleField;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class FloatField extends SingleField<Float> {

    public FloatField() {
        super("float", "Float", "null");
    }

    @Override
    public Float parseValue(String rawVal) {
        if (rawVal == null || rawVal.equals("")) {
            return 0F;
        }
        return SplitUtil.convert(Float.class, rawVal);
    }
}
