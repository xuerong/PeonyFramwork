package com.peony.engine.config.core.verify;

import com.alibaba.fastjson.JSONArray;
import com.peony.engine.config.core.ConfigException;
import com.google.common.collect.Maps;
import com.peony.engine.framework.tool.helper.ClassUtil0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * @author wjm
 */
public class VerifyFactory {
    private static final Logger logger = LoggerFactory.getLogger(VerifyFactory.class);
    private static Map<String, Class<? extends IVerify>> verifys = Maps.newConcurrentMap();

    static {
        List<Class<?>> classList = ClassUtil0.getClassList("com.peony.engine.config.core.verify.impl", true, IVerify.class, VerifyFactory.class.getClassLoader());
        for (Class<?> tempClass : classList) {
            int modifiers = tempClass.getModifiers();
            if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
                continue;
            }
            try {
                IVerify verify = (IVerify) tempClass.newInstance();
                String type = verify.getType();
                verifys.put(type, (Class<? extends IVerify>) tempClass);
                logger.info("config verify init -- {}", verify.desc());
            } catch (Exception e) {
                logger.error("verify init error " + tempClass.getSimpleName(), e);
            }
        }
    }

    public static <T extends AbstractVerify> T create(String type, JSONArray param) {
        Class<? extends IVerify> aClass = verifys.get(type);
        if (aClass != null) {
            try {
                T instance = (T) aClass.newInstance();
                instance.init(param);
                return instance;
            } catch (Exception e) {
                throw new ConfigException(e);
            }
        }
        return null;
    }
}
