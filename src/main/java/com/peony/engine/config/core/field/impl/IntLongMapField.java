package com.peony.engine.config.core.field.impl;

import com.google.common.collect.ImmutableMap;
import com.peony.engine.config.core.SplitUtil;
import com.peony.engine.config.core.field.MapField;

import java.util.Map;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class IntLongMapField extends MapField<Map<Integer,Long>> {

    public IntLongMapField() {
        super("map<int,long>", "ImmutableMap<Integer,Long>", "ImmutableMap.copyOf(new LinkedHashMap<>())");
    }

    @Override
    public Map<Integer,Long> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return ImmutableMap.copyOf(SplitUtil.convertContentToMap(rawVal.trim(), Integer.class, Long.class));
    }
}
