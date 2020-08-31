package com.peony.demo.config;

import com.google.common.base.Charsets;
import com.peony.demo.config.core.ConfigFieldMeta;
import com.peony.demo.config.core.ConfigMetaData;
import com.peony.demo.config.core.field.ConfigFieldTypeFactory;
import com.peony.demo.config.core.field.IFieldType;
import com.peony.common.tool.helper.ConfigHelper;
import com.peony.common.tool.util.Util;
import com.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jiangmin.wu on 17/7/19.
 */
public class ConfigGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigGenerator.class);
    private static Pattern extraPatten = Pattern.compile("//[ \\-]+extra[\\-]+begin[\\W\\w]+?extra[\\-]+end\\n");
    public static Map<String, String> CONFIG_NAME_ADPTER;

//    public static void main(String[] args) throws Exception {
//        File file = new File(ConfigService.CONFIG_PATH/*"./resource"*/);
//        for (File f : file.listFiles(
//                (dir, name) ->
//                name.equals("field_monster_field_monster.csv")
//                || name.equals("resource2.csv")
//                || name.equals("function_on_function_on.csv")
//                || name.equals("field_monster_refresh.csv")
//                || name.equals("item_data_config.csv") || name.equals("goods.csv") || name.equals("server_role.csv")
//        )) {
//            // 代码生成
//            try {
//                ConfigMetaData metaData = getConfigMetaData(f);
//                generateCode(metaData);
//            } catch (Exception e) {
//                logger.error("generate config error "+f.getName(), e);
//            }
//        }
//
//        // test load
//        // ConfigService.getIns().init(ConfigService.CONFIG_PATH/*"./resource"*/);
//        //java.util.concurrent.TimeUnit.DAYS.sleep(1);
//    }

    public static void generateCode(ConfigMetaData metaData, String outPath) throws Exception {
        String configName = Util.camelName(metaData.getFileName()) + "Config";
        String containerName = Util.camelName(metaData.getFileName()) + "Container";

        ConfigFieldMeta idField = metaData.getFields().get("id");

        StringBuilder configCode = new StringBuilder(1024);
        StringBuilder containerCode = new StringBuilder(1024);

        String appPackage = ConfigHelper.getString("appPackage");

        configCode.append("package "+appPackage+".config;\n");
        configCode.append("import java.util.*;\n\n");
        configCode.append("import com.alibaba.fastjson.*;\n\n");
        configCode.append("import com.peony.demo.config.core.*;\n");
        configCode.append("import com.google.common.collect.*;\n\n");

        containerCode.append(configCode);
        containerCode.append("import com.google.common.collect.*;\n\n");

        configCode.append("public class " + configName + " extends AbstractConfig<" + idField.getFieldType().getJavaType() + "> {\n");

        containerCode.append("@Config(\"" + metaData.getOrignFileName() + "\")\n");
        containerCode.append("public class " + containerName + " extends ConfigContainer<"+idField.getFieldType().getJavaType()+", " + configName + "> {\n");

        containerCode.append("\tprivate List<" + configName + "> ").append("list = ImmutableList.of();\n\n");
        containerCode.append("\tprivate Map<" + idField.getFieldType().getJavaType() + ", " + configName + "> ").append("map = ImmutableMap.of();\n\n");

        containerCode.append("\t@Override\n");
        containerCode.append("\tpublic void init(List<" + configName + "> list) {\n");
        containerCode.append("\t\tthis.list = ImmutableList.copyOf(list);\n");
        containerCode.append("\t\tMap<" + idField.getFieldType().getJavaType() + ", " + configName + "> map = new LinkedHashMap<>();\n");
        containerCode.append("\t\tfor(" + configName + " t:this.list) {\n");
        containerCode.append("\t\t\tmap.put(t.getId(), t);\n");
        containerCode.append("\t\t}\n");
        containerCode.append("\t\tthis.map = ImmutableMap.copyOf(map);\n");
        containerCode.append("\t}\n\n");

        containerCode.append("\t@Override\n");
        containerCode.append("\tpublic ").append("List<" + configName + "> ").append(" getAll")
                .append("() {\n\t\treturn list").append(";\n\t}\n\n");

        containerCode.append("\t@Override\n");
        containerCode.append("\tpublic ")
                .append("Map<" + idField.getFieldType().getJavaType() + ", " + configName + "> ").append(" getMap")
                .append("() {\n\t\treturn map").append(";\n\t}\n\n");

//        containerCode.append("\tpublic ").append(configName).append(" getConfig")
//                .append("(" + idField.getFieldType().getJavaType() + " " + idField.getName()
//                        + ") {\n\t\treturn map.get(" + idField.getName() + ")")
//                .append(";\n\t}\n");

        for (Map.Entry<String, ConfigFieldMeta> en : metaData.getFields().entrySet()) {
            IFieldType fieldType = ConfigFieldTypeFactory.getFieldType(en.getValue().getType());
            String flag = en.getValue().getFlag().toLowerCase();
            if(flag != null && !flag.contains("s")) {
                continue;
            }
            String name = en.getKey();
            if(StringUtils.isNotBlank(en.getValue().getDesc())) {
                configCode.append("\t/**\n");
                configCode.append("\t * ").append(en.getValue().getDesc()).append("\n");
                configCode.append("\t */\n");
            }
            configCode.append("\tprivate ").append(fieldType.getJavaType()).append(" ").append(name).append(" = ")
                    .append(fieldType.getDefaultVal()).append(";\n");
        }

        for (Map.Entry<String, ConfigFieldMeta> en : metaData.getFields().entrySet()) {
            IFieldType fieldType = ConfigFieldTypeFactory.getFieldType(en.getValue().getType());
            String flag = en.getValue().getFlag().toLowerCase();
            if(flag != null && !flag.contains("s")) {
                continue;
            }
            String name = en.getKey();

            if(StringUtils.isNotBlank(en.getValue().getDesc())) {
                configCode.append("\t/**\n");
                configCode.append("\t * ").append(en.getValue().getDesc()).append("\n");
                configCode.append("\t */\n");
            }

            if(name.equals("id")) {
                configCode.append("\t@Override\n");
            }

            configCode.append("\tpublic ").append(fieldType.getJavaType()).append(fieldType.getName().equalsIgnoreCase("bool") ? " is" : " get")
                    .append(Util.camelName(name)).append("() {\n\t\treturn ").append(name).append(";\n\t}\n\n");
        }

        // tojson
        StringBuilder toJson = new StringBuilder();
        toJson.append("\tpublic JSONObject toJsonObj() {\n");
        toJson.append("\t\tJSONObject obj = new JSONObject();\n");
        for (Map.Entry<String, ConfigFieldMeta> en : metaData.getFields().entrySet()) {
            IFieldType fieldType = ConfigFieldTypeFactory.getFieldType(en.getValue().getType());
            String flag = en.getValue().getFlag().toLowerCase();
            if(flag != null && !flag.contains("s")) {
                continue;
            }
            String name = en.getKey();
            toJson.append("\t\tobj.put(\""+name+"\", "+name+");\n");
        }
        toJson.append("\t\treturn obj;\n");
        toJson.append("\t}\n\n");

        String  path = appPackage.replace(".","/");

        File configFile = new File(outPath + "src/main/java/"+path+"/config/" + configName + ".java");
        String extraContent = null;
        if (configFile.exists()) {
            Matcher matcher = extraPatten.matcher(FileUtils.readFileToString(configFile, Charsets.UTF_8));
            if (matcher.find()) {
                extraContent = matcher.group();
            }
        }

        if (extraContent == null) {
            extraContent = "// --------------------------------------extra--begin\n\n" + toJson.toString() + "\n\t// --------------------------------------extra--end\n\n";
        }

        // extra
        configCode.append("\n\n\t").append(extraContent);

        configCode.append("}");
        containerCode.append("}");

        // config
        FileUtils.writeStringToFile(configFile, configCode.toString(), Charsets.UTF_8);



        File containerFile = new File(outPath + "src/main/java/"+path+"/config/" + containerName + ".java");
        if (!containerFile.exists()) {
            FileUtils.writeStringToFile(containerFile, containerCode.toString(), Charsets.UTF_8);
        }
    }

    public ConfigMetaData getConfigMetaData(File file) {
        CSVReader csvReader = null;
        ConfigMetaData metaData = new ConfigMetaData();
        try {
            String tableName = file.getName(); // TODO 这里做了个修改，取消了小写转换，不知道之前为啥有小写转换
            tableName = tableName.substring(0, tableName.indexOf("."));

            metaData.setOrignFileName(tableName);

            if(CONFIG_NAME_ADPTER.containsKey(tableName)) {
                metaData.setFileName(CONFIG_NAME_ADPTER.get(tableName));
            }else{
                metaData.setFileName(tableName);
            }

            csvReader = new CSVReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8), '`');

            List<String[]> content = csvReader.readAll();
            String[] descs = content.get(0); // descs
            String[] coltypes = content.get(1); // type
            String[] colnames = content.get(0); // name
//            String[] verifies = content.get(3); // verify

            Map<String, ConfigFieldMeta> fields = new LinkedHashMap<>();
            for (int k = 0; k < coltypes.length; k++) {
                coltypes[k] = coltypes[k].trim();
                if (k == 0) {
                    colnames[k] = colnames[k].toLowerCase();
                }
                if (coltypes[k].isEmpty()) {
                    continue;
                }
                // colnames[k] = colnames[k].toLowerCase();
                ConfigFieldMeta field = new ConfigFieldMeta(null, tableName, coltypes[k], descs[k], colnames[k],
                        "",//verifies[k],
                        null);
                fields.put(field.getName(), field);
            }
            metaData.setFields(fields);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (csvReader != null) {
                try {
                    csvReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return metaData;
    }

}
