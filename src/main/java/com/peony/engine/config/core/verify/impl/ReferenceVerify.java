package com.peony.engine.config.core.verify.impl;

import com.alibaba.fastjson.JSONArray;
import com.peony.engine.config.core.ConfigContainer;
import com.peony.engine.config.core.ConfigException;
import com.peony.engine.config.core.ConfigFieldMeta;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.core.verify.AbstractVerify;
import com.peony.engine.config.core.verify.VerifyContext;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * 例子：
 * {t:'ref',p:[reward,id]}
 * 当前字段引用自 reward表的id字段
 * 此类只负责处理解析 reward-id 和验证
 *
 * @author wjm
 */
public class ReferenceVerify extends AbstractVerify {

    public ReferenceVerify() {
        super("ref");
    }

    @Override
    public void init(JSONArray param) {
        tableName = param.getString(0);
        fieldName = param.getString(1);
    }

    @Override
    public String desc() {
        return "引用值验证: {t:'ref', p:['表名','列名']}";
    }

    private String tableName;
    private String fieldName;

    private Set<Object> refValSet;

    @SuppressWarnings("rawtypes")
    @Override
    public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta cfgField, Object val, ConfigContainer selfContainer,
                         boolean exception) throws Exception {

        if (val == null || StringUtils.isBlank(val.toString())) {
            return null;
        }

        ConfigContainer ref = context.getConfigContainer(tableName);
        ConfigFieldMeta refField = ref.getMetaData().getFields().get(fieldName);
        initRefValues(ref, refField);

        boolean exists = false;
        if (refValSet.contains(val)) {
            exists = true;
        }

        if (!exists) {
            String error = String.format("引用不存在([%s %s]) %s %s %s %s",
                    ref.getMetaData().getFileName(), refField.getName(),
                    selfContainer.getMetaData().getFileName(), cfgField.getName(), configItem.getId(), val);
            if (exception) {
                throw new ConfigException(error);
            } else {
                return error;
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private void initRefValues(ConfigContainer<?, IConfig<?>> ref, ConfigFieldMeta refField)
            throws NoSuchFieldException, IllegalAccessException {
        if (refValSet != null) {
            return;
        }
        Set<Object> refValSet = Sets.newHashSet();
        for (IConfig tpl : ref.getAll()) {
            Field f = tpl.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);

            Object v = f.get(tpl);
            if (v == null || StringUtils.isBlank(v.toString())) {
                continue;
            }

            refValSet.add(v);
        }
        this.refValSet = refValSet;
    }
}
