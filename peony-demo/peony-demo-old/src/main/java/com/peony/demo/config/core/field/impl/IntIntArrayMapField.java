package com.peony.demo.config.core.field.impl;

import com.peony.demo.config.core.SplitUtil;
import com.peony.demo.config.core.field.MapField;

import java.util.List;
import java.util.Map;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class IntIntArrayMapField extends MapField<Map<Integer, List<Integer>>> {

    public IntIntArrayMapField() {
        super("map<int,array<int>>", "ImmutableMap<Integer,ImmutableList<Integer>>", "ImmutableMap.copyOf(new LinkedHashMap<>())");
    }

    @Override
    public Map<Integer, List<Integer>> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return SplitUtil.convertContentToMapList(rawVal.trim(), Integer.class, Integer.class);
    }

//    @Override
//    protected StringBuilder verifyPart2(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, Map<String, List<Integer>> value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
//        for (List<Integer> val : value.values()) {
//            for (Integer v : val) {
//                String result = verify.verify(context, configItem, fieldMeta, v, selfContainer, exception);
//                log = appendLog(log, result);
//            }
//        }
//        return log;
//    }
}
