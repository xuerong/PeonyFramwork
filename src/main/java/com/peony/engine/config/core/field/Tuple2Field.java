package com.peony.engine.config.core.field;

import com.peony.engine.config.core.ConfigContainer;
import com.peony.engine.config.core.ConfigFieldMeta;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.core.verify.IVerify;
import com.peony.engine.config.core.verify.VerifyContext;
import com.peony.engine.config.core.Tuple2;

/**
 * Created by jiangmin.wu on 2018/3/6.
 */
public abstract class Tuple2Field<T extends Tuple2> extends TwoPartField<T> {

    public Tuple2Field(String name, String javaType, String defaultVal) {
        super(name, javaType, defaultVal, "f", "s");
    }

    @Override
    protected StringBuilder verifyPart1(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        String result = verify.verify(context, configItem, fieldMeta, value.getFirst(), selfContainer, exception);
        log = appendLog(log, result);
        return log;
    }

    @Override
    protected StringBuilder verifyPart2(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        String result = verify.verify(context, configItem, fieldMeta, value.getSecond(), selfContainer, exception);
        log = appendLog(log, result);
        return log;
    }
}
