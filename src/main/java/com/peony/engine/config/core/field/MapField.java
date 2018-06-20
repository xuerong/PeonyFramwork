package com.peony.engine.config.core.field;

import com.peony.engine.config.core.ConfigContainer;
import com.peony.engine.config.core.ConfigFieldMeta;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.core.verify.IVerify;
import com.peony.engine.config.core.verify.VerifyContext;

import java.util.Map;

/**
 * Created by jiangmin.wu on 2018/3/6.
 */
public abstract class MapField<T extends Map> extends TwoPartField<T> {

    public MapField(String name, String javaType, String defaultVal) {
        super(name, javaType, defaultVal, "k", "v");
    }

    @Override
    protected StringBuilder verifyPart1(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        for (Object key : value.keySet()) {
            String result = verify.verify(context, configItem, fieldMeta, key, selfContainer, exception);
            log = appendLog(log, result);
        }
        return log;
    }

    @Override
    protected StringBuilder verifyPart2(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception {
        for (Object val : value.values()) {
            String result = verify.verify(context, configItem, fieldMeta, val, selfContainer, exception);
            log = appendLog(log, result);
        }
        return log;
    }
}
