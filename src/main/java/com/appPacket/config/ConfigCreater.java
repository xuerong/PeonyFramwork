package com.appPacket.config;

import com.peony.engine.config.ConfigGenerator;
import com.peony.engine.config.ConfigService;
import com.peony.engine.config.core.ConfigMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jiangmin.wu
 */
public class ConfigCreater {
    private static final Logger logger = LoggerFactory.getLogger(ConfigGenerator.class);

    public static List<String> getList(){
        ArrayList<String> list = new ArrayList<String>(){
            {
                add("test");

            }
        };
        return list;
    }

    public static Map<String, String> CONFIG_NAME_ADPTER = new HashMap<String, String>() {

            // 将需要生成的配置文件添加到下面, 然后运行该 main 方法即可.
            //
        {   // 原始表名                               程序中用的名称
            for(String str: getList()){
                put(str, str);
            }
        }
    };

    public static void main(String[] args) {
        String rootPath = "./";

        ConfigGenerator.CONFIG_NAME_ADPTER = CONFIG_NAME_ADPTER;
//        File file = new File(rootPath + ConfigService.CONFIG_PATH);
        File file = new File(ConfigService.CONFIG_PATH2);
        for (File f : file.listFiles(
                (dir, name) -> {
                    String fileName = name.substring(0, name.indexOf("."));
                    return ConfigGenerator.CONFIG_NAME_ADPTER.containsKey(fileName);
                })) {
            try {
                ConfigMetaData metaData = new ConfigGenerator().getConfigMetaData( f);
                ConfigGenerator.generateCode(metaData, "./");
            } catch (Exception e) {
                logger.error("generate config error " + f.getName(), e);
            }
        }
    }
}
