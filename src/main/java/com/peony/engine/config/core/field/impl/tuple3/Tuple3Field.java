package com.peony.engine.config.core.field.impl.tuple3;

import com.peony.engine.config.core.*;
import com.peony.engine.config.core.field.ThreePartField;
import com.peony.engine.config.core.verify.IVerify;
import com.peony.engine.config.core.verify.VerifyContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public abstract class Tuple3Field<F, S, T> extends ThreePartField<Tuple3<F, S, T>> {

    Type[] arguments;

    public Tuple3Field(String name, String javaType) {
        super(name, javaType, "null", "f", "s", "t");
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        arguments = parameterizedType.getActualTypeArguments();
    }

    @Override
    public Tuple3<F, S, T> parseValue(String rawVal) {
        if (rawVal == null) {
            return null;
        }
        return SplitUtil.convertContentToTuple3(rawVal, SplitUtil.SEPARATOR_2, (Class<F>) arguments[0], (Class<S>) arguments[1], (Class<T>) arguments[2]);
    }

    @Override
    protected StringBuilder verifyPart1(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, Tuple3<F, S, T> value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        String result = verify.verify(context, configItem, fieldMeta, value.getFirst(), selfContainer, exception);
        log = appendLog(log, result);
        return log;
    }

    @Override
    protected StringBuilder verifyPart2(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, Tuple3<F, S, T> value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        String result = verify.verify(context, configItem, fieldMeta, value.getSecond(), selfContainer, exception);
        log = appendLog(log, result);
        return log;
    }

    @Override
    protected StringBuilder verifyPart3(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, Tuple3<F, S, T> value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        String result = verify.verify(context, configItem, fieldMeta, value.getThird(), selfContainer, exception);
        log = appendLog(log, result);
        return log;
    }
}
