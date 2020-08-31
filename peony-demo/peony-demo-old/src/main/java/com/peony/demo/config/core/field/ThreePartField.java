package com.peony.demo.config.core.field;

import com.alibaba.fastjson.JSONObject;
import com.peony.demo.config.core.ConfigContainer;
import com.peony.demo.config.core.ConfigFieldMeta;
import com.peony.demo.config.core.IConfig;
import com.peony.demo.config.core.verify.IVerify;
import com.peony.demo.config.core.verify.VerifyContext;
import com.peony.demo.config.core.ConfigException;
import com.google.api.client.util.Lists;

import java.util.List;

/**
 * 包含3个字段的值类型, 如: map<int,map<>>, tuple3<int,int,int> ...
 * <p>
 * Created by jiangmin.wu on 2018/3/6.
 */
public abstract class ThreePartField<E> extends TwoPartField<E> {


    protected List<IVerify> verifyList3 = Lists.newArrayList();

    protected String verifyKey3;

    public ThreePartField(String name, String javaType, String defaultVal, String verifyKey1, String verifyKey2, String verifyKey3) {
        super(name, javaType, defaultVal, verifyKey1, verifyKey2);
        this.verifyKey3 = verifyKey3;
    }

    @Override
    public void beforeVerify(ConfigFieldMeta fieldMeta) throws Exception {
        Object verifyConfig = fieldMeta.getVerifyConfig();
        if (verifyConfig == null) {
            return;
        }

        if (!(verifyConfig instanceof JSONObject)) {
            throw new ConfigException(String.format("验证项格式错误 %s %s    %s 字段必须分别制定 %s,%s,%s 例如:{%s:{t:unique},%s:[{t:unique},{t:'range',p:[1000,2000]}],%s:[{t:unique}]}", fieldMeta.getFileName(), fieldMeta.getName(), fieldMeta.getType(), verifyKey1, verifyKey2, verifyKey3, verifyKey1, verifyKey2, verifyKey3));
        }

        initVerify(verifyConfig, verifyKey1, verifyList1);
        initVerify(verifyConfig, verifyKey2, verifyList2);
        initVerify(verifyConfig, verifyKey3, verifyList3);
    }

    @Override
    public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, E value, ConfigContainer selfContainer, boolean exception) throws Exception {
        String result = super.verify(context, configItem, fieldMeta, value, selfContainer, exception);
        StringBuilder log = appendLog(null, result);

        // value
        for (IVerify verify : verifyList3) {
            if (value == null) {
                result = verify.verify(context, configItem, fieldMeta, value, selfContainer, exception);
                log = appendLog(log, result);
            } else {
                log = verifyPart3(context, configItem, fieldMeta, value, selfContainer, exception, log, verify);
            }
        }
        return log != null ? log.toString() : null;
    }

    protected abstract StringBuilder verifyPart3(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, E value, ConfigContainer selfContainer, boolean exception, StringBuilder log, IVerify verify) throws Exception;
}