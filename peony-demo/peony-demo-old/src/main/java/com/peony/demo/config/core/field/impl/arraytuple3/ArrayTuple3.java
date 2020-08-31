package com.peony.demo.config.core.field.impl.arraytuple3;

import com.peony.demo.config.core.*;
import com.peony.demo.config.core.field.ArrayThreePartField;
import com.peony.demo.config.core.verify.IVerify;
import com.peony.demo.config.core.verify.VerifyContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public abstract class ArrayTuple3<F,S,T> extends ArrayThreePartField<List<Tuple3<F,S,T>>> {
    Type[] arguments;
    public ArrayTuple3(String name, String javaType) {
        super(name, javaType, "null", "f","s","t");
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        arguments = parameterizedType.getActualTypeArguments();
    }

    @Override
    public List<Tuple3<F,S,T>> parseValue(String rawVal) {
        if(rawVal == null) {
            return null;
        }
        return SplitUtil.convertContentToTuple3List(rawVal, SplitUtil.SEPARATOR_2, SplitUtil.SEPARATOR_1, (Class<F>) arguments[0], (Class<S>)arguments[1], (Class<T>)arguments[2]);
    }

    @Override
    protected StringBuilder verifyPart1(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, List<Tuple3<F,S,T>> value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        if(value == null) {
            String result = verify.verify(context, configItem, fieldMeta, value, selfContainer, exception);
            log = appendLog(log, result);
        } else {
            for(Tuple3 tuple3:value) {
                String result = verify.verify(context, configItem, fieldMeta, tuple3.getFirst(), selfContainer, exception);
                log = appendLog(log, result);
            }
        }
        return log;
    }

    @Override
    protected StringBuilder verifyPart2(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, List<Tuple3<F,S,T>> value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        if(value == null) {
            String result = verify.verify(context, configItem, fieldMeta, value, selfContainer, exception);
            log = appendLog(log, result);
        } else {
            for(Tuple3 tuple3:value) {
                String result = verify.verify(context, configItem, fieldMeta, tuple3.getSecond(), selfContainer, exception);
                log = appendLog(log, result);
            }
        }
        return log;
    }

    @Override
    protected StringBuilder verifyPart3(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, List<Tuple3<F,S,T>> value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        if(value == null) {
            String result = verify.verify(context, configItem, fieldMeta, value, selfContainer, exception);
            log = appendLog(log, result);
        } else {
            for(Tuple3 tuple3:value) {
                String result = verify.verify(context, configItem, fieldMeta, tuple3.getThird(), selfContainer, exception);
                log = appendLog(log, result);
            }
        }
        return log;
    }
}
