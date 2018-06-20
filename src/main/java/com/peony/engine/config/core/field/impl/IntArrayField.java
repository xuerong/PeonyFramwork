package com.peony.engine.config.core.field.impl;

import com.google.common.collect.ImmutableList;
import com.peony.engine.config.core.SplitUtil;
import com.peony.engine.config.core.field.ArrayField;

import java.util.List;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class IntArrayField extends ArrayField<List<Integer>> {

    public IntArrayField() {
        super("array<int>", "ImmutableList<Integer>", "ImmutableList.copyOf(new ArrayList<>())");
    }

    @Override
    public List<Integer> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return ImmutableList.copyOf(SplitUtil.convertContentToList(rawVal.trim(), Integer.class));
    }
}
