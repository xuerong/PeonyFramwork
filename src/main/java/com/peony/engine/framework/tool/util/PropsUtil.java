package com.peony.engine.framework.tool.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * 属性文件操作工具类
 *
 * @author huangyong
 * @since 1.0
 */
public class PropsUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropsUtil.class);

    /**
     * 加载属性文件
     */
    public static Properties loadProps(String propsPath) {
        Properties props = new Properties();
        InputStreamReader is = null;
        try {
            if (StringUtils.isEmpty(propsPath)) {
                throw new IllegalArgumentException();
            }
            String suffix = ".properties";
            if (propsPath.lastIndexOf(suffix) == -1) {
                propsPath += suffix;
            }
            is = new InputStreamReader(ClassUtil.getClassLoader().getResourceAsStream(propsPath), "UTF-8");
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            logger.error("加载属性文件出错！", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                logger.error("释放资源出错！", e);
            }
        }
        return props;
    }

    /**
     * 加载一个目录下的属性文件
     */
    public static Map<String,Properties> loadDirProps(String dirPath) {
        Map<String,Properties> result = new HashMap<>();
        try {
            URL url = ClassUtil.getClassLoader().getResource(dirPath);
            File file = new File(url.toURI());
            String[] fileList = file.list();
            for (String fileName : fileList) {
                if(fileName.endsWith("properties")){
                    result.put(fileName.replace(".properties",""),loadProps(dirPath+"/"+fileName));
                }
            }
        }catch (Throwable e){
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 加载属性文件，并转为 Map
     */
    public static Map<String, String> loadPropsToMap(String propsPath) {
        Map<String, String> map = new HashMap<String, String>();
        Properties props = loadProps(propsPath);
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
        return map;
    }

    /**
     * 获取字符型属性
     */
    public static String getString(Properties props, String key) {
        String value = "";
        if (props.containsKey(key)) {
            value = props.getProperty(key);
        }
        return value;
    }

    /**
     * 获取字符型属性（带有默认值）
     */
    public static String getString(Properties props, String key, String defalutValue) {
        String value = defalutValue;
        if (props.containsKey(key)) {
            value = props.getProperty(key);
        }
        return value;
    }

    /**
     * 获取数值型属性
     */
    public static int getNumber(Properties props, String key) {
        int value = 0;
        if (props.containsKey(key)) {
            if(StringUtils.isNumeric(props.getProperty(key))) {
                value = Integer.parseInt(props.getProperty(key));
            }
        }
        return value;
    }

    // 获取数值型属性（带有默认值）
    public static int getNumber(Properties props, String key, int defaultValue) {
        int value = defaultValue;
        if (props.containsKey(key)) {
            if(StringUtils.isNumeric(props.getProperty(key))) {
                value = Integer.parseInt(props.getProperty(key));
            }
        }
        return value;
    }

    /**
     * 获取布尔型属性
     */
    public static boolean getBoolean(Properties props, String key) {
        return getBoolean(props, key, false);
    }

    /**
     * 获取布尔型属性（带有默认值）
     */
    public static boolean getBoolean(Properties props, String key, boolean defalutValue) {
        boolean value = defalutValue;
        if (props.containsKey(key)) {
            String vauleStr=props.getProperty(key).trim();
            if(vauleStr.equals("true") || vauleStr.equals("false")) {
                value = Boolean.parseBoolean(vauleStr);
            }
        }
        return value;
    }

    /**
     * 获取指定前缀的相关属性
     */
    public static Map<String, String> getMap(Properties props, String prefix) {
        Map<String, String> kvMap = new LinkedHashMap<String, String>();
        Set<String> keySet = props.stringPropertyNames();
        if (CollectionUtils.isNotEmpty(keySet)) {
            for (String key : keySet) {
                if (key.startsWith(prefix)) {
                    String value = props.getProperty(key);
                    kvMap.put(key, value);
                }
            }
        }
        return kvMap;
    }
}
