package com.peony.engine.config.core.verify.impl;

import com.alibaba.fastjson.JSONArray;
import com.peony.engine.config.core.ConfigContainer;
import com.peony.engine.config.core.ConfigException;
import com.peony.engine.config.core.ConfigFieldMeta;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.core.verify.AbstractVerify;
import com.peony.engine.config.core.verify.VerifyContext;

import java.util.regex.Pattern;

/**
 * @author wjm
 */
public class Format extends AbstractVerify {

    public Format() {
        super("format");
    }

    private Pattern pattern;

    @Override
    public void init(JSONArray param) {
        pattern = Pattern.compile(param.getString(0));
    }

    @Override
    public String desc() {
        return "格式验证: {t:'format', p:['格式正则']}";
    }

    @Override
    public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta cfgField, Object val,
                         ConfigContainer selfContainer, boolean exception) throws Exception {

        if (val != null && !pattern.matcher(val.toString()).matches()) {
            String error = String.format("字段格式不正确 %s %s %s",
                    selfContainer.getMetaData().getFileName(), cfgField.getName(), configItem.getId(), val);
            if (exception) {
                throw new ConfigException(error);
            } else {
                return error;
            }
        }

        return null;
    }
}
