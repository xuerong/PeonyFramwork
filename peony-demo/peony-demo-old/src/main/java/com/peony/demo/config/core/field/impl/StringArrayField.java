package com.peony.demo.config.core.field.impl;

import com.peony.demo.config.core.field.ArrayField;
import com.peony.demo.config.core.SplitUtil;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class StringArrayField extends ArrayField<List<String>> {

    public StringArrayField() {
        super("array<string>", "ImmutableList<String>", "ImmutableList.copyOf(new ArrayList<>())");
    }

    @Override
    public List<String> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return ImmutableList.copyOf(SplitUtil.convertContentToList(rawVal.trim(), String.class));
    }
}
