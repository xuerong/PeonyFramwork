package com.peony.engine.config.core.verify;

import java.util.Map;

import com.google.api.client.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.peony.engine.config.core.ConfigContainer;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.ConfigService;

/**
 * @author wjm
 */
public class VerifyContext {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private Map<Class<?>, ConfigContainer<?, IConfig<?>>> configContainers;
    private Map<String, ConfigContainer<?, IConfig<?>>> configContainersByFileName;

    public VerifyContext(Map<Class<?>, ConfigContainer<?, IConfig<?>>> configContainers) {
        this.configContainers = configContainers;
        this.configContainersByFileName = Maps.newHashMap();
        for (ConfigContainer<?, IConfig<?>> t : configContainers.values()) {
            this.configContainersByFileName.put(t.getMetaData().getFileName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfigContainer(String name) {
        T t = (T) this.configContainersByFileName.get(name);
        if (t == null) {
            logger.error("config [{}.csv] not found !", name);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfigContainer(Class<T> clazz) {
        return (T) configContainers.get(clazz);
    }
}
