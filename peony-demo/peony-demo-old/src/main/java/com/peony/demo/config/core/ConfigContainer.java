package com.peony.demo.config.core;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.peony.demo.config.core.field.IFieldType;
import org.apache.commons.lang3.StringUtils;

import com.peony.demo.config.core.verify.VerifyContext;

/**
 * Created by jiangmin.wu on 17/7/20.
 */
public abstract class ConfigContainer<K, T extends IConfig<K>> {
    private ConfigMetaData metaData;

    public abstract void init(List<T> list);

    public abstract List<T> getAll();

    public abstract Map<K, T> getMap();

    public T getConfig(K id) {
        return getMap().get(id);
    }

    public ConfigMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(ConfigMetaData metaData) {
        this.metaData = metaData;
    }

    public String check(VerifyContext context, boolean exception) throws Exception {
        // 手动检查
        return null;
    }

    @SuppressWarnings("rawtypes")
    public String verify(VerifyContext context, boolean exception) throws Exception {
        StringBuilder log = new StringBuilder();
        for (ConfigFieldMeta field : metaData.getFields().values()) {
            String flag = field.getFlag();
            if (flag != null && !flag.contains("s")) {
                continue;
            }

            if (null != field.getVerifyConfig()) {

                IFieldType fieldType = field.getFieldType();
                fieldType.beforeVerify(field);

                for (Object t : getAll()) {
                    IConfig<?> item = (IConfig<?>) t;

                    Field itemField = item.getClass().getDeclaredField(field.getName());
                    itemField.setAccessible(true);
                    Object value = itemField.get(item);

                    String ret = fieldType.verify(context, item, field, value, this, exception);
                    if (ret != null && StringUtils.isNotBlank(ret)) {
                        log.append(ret).append("\n");
                    }
                }
            }
        }

        return log.toString();
    }
}
