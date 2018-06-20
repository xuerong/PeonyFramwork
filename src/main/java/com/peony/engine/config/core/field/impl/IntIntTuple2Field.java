package com.peony.engine.config.core.field.impl;

import com.peony.engine.config.core.field.Tuple2Field;
import com.peony.engine.config.core.SplitUtil;
import com.peony.engine.config.core.Tuple2;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class IntIntTuple2Field extends Tuple2Field<Tuple2<Integer, Integer>> {

    public IntIntTuple2Field() {
        super("tuple2<int,int>", "Tuple2<Integer,Integer>", "null");
    }

    @Override
    public Tuple2<Integer, Integer> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return SplitUtil.convertContentToTuple2(rawVal, Integer.class, Integer.class);
    }
}
