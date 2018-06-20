package com.peony.engine.config.core.field.impl;

import com.peony.engine.config.core.field.MapField;
import com.peony.engine.config.core.SplitUtil;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class IntIntMapField extends MapField<Map<Integer,Integer>> {

    public IntIntMapField() {
        super("map<int,int>", "ImmutableMap<Integer,Integer>", "ImmutableMap.copyOf(new LinkedHashMap<>())");
    }

    @Override
    public Map<Integer,Integer> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return ImmutableMap.copyOf(SplitUtil.convertContentToMap(rawVal.trim(), Integer.class, Integer.class));
    }
}
