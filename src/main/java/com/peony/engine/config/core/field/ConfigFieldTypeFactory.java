package com.peony.engine.config.core.field;

import com.google.api.client.util.Maps;
import com.peony.engine.framework.tool.helper.ClassUtil0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * Created by jiangmin.wu on 2018/3/7.
 */
public class ConfigFieldTypeFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFieldTypeFactory.class);

    private static Map<String, IFieldType> fieldTypeMap = Maps.newHashMap();
    private static Map<String, Class<? extends IFieldType>> fieldTypeClassMap = Maps.newHashMap();

    static {
        List<Class<?>> classList = ClassUtil0.getClassList("com.peony.engine.config.core.field.impl", true, IFieldType.class, ConfigFieldTypeFactory.class.getClassLoader());
        for (Class<?> tempClass : classList) {
            int modifiers = tempClass.getModifiers();
            if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
                continue;
            }
            try {
                IFieldType fieldType = (IFieldType) tempClass.newInstance();
                fieldTypeMap.put(fieldType.getName(), fieldType);
                fieldTypeClassMap.put(fieldType.getName(), (Class<? extends IFieldType>) tempClass);
                logger.info("config type init -- {}", fieldType.getName());
            } catch (Exception e) {
                logger.error("verify init error " + tempClass.getSimpleName(), e);
            }
        }
    }

    public static IFieldType getFieldType(String typeName) {
        return fieldTypeMap.get(typeName);
    }

    public static Class<? extends IFieldType> getFieldTypeClass(String typeName) {
        return fieldTypeClassMap.get(typeName);
    }
}
