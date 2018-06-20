package com.peony.engine.config.core.field.impl;

import com.peony.engine.config.core.field.MapField;
import com.peony.engine.config.core.SplitUtil;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class StrStrMapField extends MapField<Map<String,String>> {

    public StrStrMapField() {
        super("map<string,string>", "ImmutableMap<String,String>", "ImmutableMap.copyOf(new LinkedHashMap<>())");
    }

    @Override
    public Map<String,String> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return ImmutableMap.copyOf(SplitUtil.convertContentToMap(rawVal.trim(), String.class, String.class));
    }
}
