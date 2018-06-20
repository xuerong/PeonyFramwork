package com.peony.engine.config.core.field;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peony.engine.config.core.ConfigContainer;
import com.peony.engine.config.core.ConfigFieldMeta;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.core.verify.IVerify;
import com.peony.engine.config.core.verify.VerifyContext;
import com.google.api.client.util.Lists;

import java.util.List;

/**
 * Created by jiangmin.wu on 2018/3/6.
 */
public abstract class SingleField<T> extends AbstractFieldType<T> {

    protected List<IVerify> verifyList = Lists.newArrayList();

    public SingleField(String name, String javaType, String defaultVal) {
        super(name, javaType, defaultVal);
    }

    @Override
    public void beforeVerify(ConfigFieldMeta fieldMeta) throws Exception {
        Object verifyConfig = fieldMeta.getVerifyConfig();
        if (verifyConfig == null) {
            return;
        }

        // 多个验证
        if (verifyConfig instanceof JSONArray) {
            JSONArray jsonArrayCfg = (JSONArray) verifyConfig;

            for (int i = 0; i < jsonArrayCfg.size(); i++) {
                verifyList.add(createVerity((JSONObject) jsonArrayCfg.get(i)));
            }
        } else if (verifyConfig instanceof JSONObject) {
            verifyList.add(createVerity((JSONObject) verifyConfig));
        }
    }

    @Override
    public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception) throws Exception {
        return innerVerify(context, configItem, fieldMeta, value, selfContainer, exception);
    }

    protected String innerVerify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, Object value, ConfigContainer selfContainer, boolean exception) throws Exception {
        StringBuilder log = null;
        for (IVerify verify : verifyList) {
            String result = verify.verify(context, configItem, fieldMeta, value, selfContainer, exception);
            log = appendLog(log, result);
        }
        return log != null ? log.toString() : null;
    }
}
