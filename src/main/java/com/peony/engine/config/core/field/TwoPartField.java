package com.peony.engine.config.core.field;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peony.engine.config.core.ConfigContainer;
import com.peony.engine.config.core.ConfigFieldMeta;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.core.verify.IVerify;
import com.peony.engine.config.core.verify.VerifyContext;
import com.peony.engine.config.core.ConfigException;
import com.google.api.client.util.Lists;

import java.util.List;

/**
 * 包含两个字段的值类型, 如: map, tuple2
 * <p>
 * Created by jiangmin.wu on 2018/3/6.
 */
public abstract class TwoPartField<T> extends AbstractFieldType<T> {

    protected List<IVerify> verifyList1 = Lists.newArrayList();
    protected List<IVerify> verifyList2 = Lists.newArrayList();

    protected String verifyKey1;
    protected String verifyKey2;

    public TwoPartField(String name, String javaType, String defaultVal, String verifyKey1, String verifyKey2) {
        super(name, javaType, defaultVal);
        this.verifyKey1 = verifyKey1;
        this.verifyKey2 = verifyKey2;
    }

    @Override
    public void beforeVerify(ConfigFieldMeta fieldMeta) throws Exception {
        Object verifyConfig = fieldMeta.getVerifyConfig();
        if (verifyConfig == null) {
            return;
        }

        if (!(verifyConfig instanceof JSONObject)) {
            throw new ConfigException(String.format("验证项格式错误 %s %s    %s 字段必须分别制定 %s,%s 例如:{%s:{t:unique},%s:[{t:unique},{t:'range',p:[1000,2000]}]}", fieldMeta.getFileName(), fieldMeta.getName(), fieldMeta.getType(), verifyKey1, verifyKey2, verifyKey1, verifyKey2));
        }

        initVerify(verifyConfig, verifyKey1, verifyList1);
        initVerify(verifyConfig, verifyKey2, verifyList2);
    }

    protected void initVerify(Object verifyConfig, String key, List<IVerify> verifyList) {
        Object kCfg = ((JSONObject) verifyConfig).get(key);
        if (kCfg != null) {
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
    }

    @Override
    public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception) throws Exception {
        StringBuilder log = null;

        // key
        for (IVerify verify : verifyList1) {
            if (value == null) {
                String result = verify.verify(context, configItem, fieldMeta, value, selfContainer, exception);
                log = appendLog(log, result);
            } else {
                log = verifyPart1(context, configItem, fieldMeta, value, selfContainer, exception, log, verify);
            }
        }

        // value
        for (IVerify verify : verifyList2) {
            if (value == null) {
                String result = verify.verify(context, configItem, fieldMeta, value, selfContainer, exception);
                log = appendLog(log, result);
            } else {
                log = verifyPart2(context, configItem, fieldMeta, value, selfContainer, exception, log, verify);
            }
        }
        return log != null ? log.toString() : null;
    }

    protected abstract StringBuilder verifyPart1(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception;

    protected abstract StringBuilder verifyPart2(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception;
}
