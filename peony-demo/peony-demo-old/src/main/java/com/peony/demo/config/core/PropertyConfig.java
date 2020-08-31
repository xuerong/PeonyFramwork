package com.peony.demo.config.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyConfig {
    private static Logger logger = LoggerFactory.getLogger(PropertyConfig.class);

    private Map<String, Properties> propertiesMap = new HashMap<>();

    /**
     * 支持多文件加载
     *
     * @throws Exception
     */
    public synchronized void loadConfig(String configPath) throws Exception {

        final Properties properties = new Properties();
        properties.load(new FileInputStream(new File(configPath)));

        if (!propertiesMap.isEmpty()) {
            // 检查重复 key
            propertiesMap.forEach((path, prop) -> {
                // 自身不检查了
                if (path.equals(configPath)) {
                    return;
                }
                prop.forEach((k, v) -> {
                    if (properties.containsKey(k)) {
                        throw new ConfigException(String.format("properties key 重复 %s %s %s", path, configPath, k));
                    }
                });
            });
        }

        Map<String, Properties> map = Maps.newHashMap(propertiesMap);
        map.put(configPath, properties);

        propertiesMap = map;

        logger.info("load config {} finish", configPath);
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        if (propertiesMap.isEmpty()) {
            return defaultValue;
        }

        if (propertiesMap.size() == 1) {
            return propertiesMap.values().iterator().next().getProperty(key, defaultValue);
        }

        for (Properties p : propertiesMap.values()) {
            if (p.containsKey(key)) {
                return p.getProperty(key);
            }
        }

        return defaultValue;
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key, String.valueOf(defaultValue));
        return Integer.parseInt(val);
    }

    public boolean getBool(String key) {
        return Boolean.valueOf(get(key));
    }

    public boolean getBool(String key, int defaultValue) {
        String val = get(key, String.valueOf(defaultValue));
        return Boolean.valueOf(val);
    }

    public long getLong(String key) {
        return Long.parseLong(get(key));
    }

    public long getLong(String key, long defaultValue) {
        String val = get(key, String.valueOf(defaultValue));
        return Long.parseLong(val);
    }
}
