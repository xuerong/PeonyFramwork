package com.peony.demo.config.core.field.impl;

import com.peony.demo.config.core.field.Tuple2Field;
import com.peony.demo.config.core.SplitUtil;
import com.peony.demo.config.core.Tuple2;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class IntStrTuple2Field extends Tuple2Field<Tuple2<Integer, String>> {

    public IntStrTuple2Field() {
        super("tuple2<int,string>", "Tuple2<Integer,String>", "null");
    }

    @Override
    public Tuple2<Integer, String> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return SplitUtil.convertContentToTuple2(rawVal, Integer.class, String.class);
    }
}
