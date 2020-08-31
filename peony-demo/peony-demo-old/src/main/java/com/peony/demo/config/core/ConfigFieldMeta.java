package com.peony.demo.config.core;

import com.alibaba.fastjson.JSON;
import com.peony.demo.config.ConfigService;
import com.peony.demo.config.core.field.ConfigFieldTypeFactory;
import com.peony.demo.config.core.field.IFieldType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jiangmin.wu on 17/7/19.
 */
public class ConfigFieldMeta {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFieldMeta.class);

    private String fileName;
    private String type;

    private Object verifyConfig;

    private String desc;
    private String name;
    private String flag = "s";

    public ConfigFieldMeta(ConfigService configService, String fileName, String type, String desc, String name, String verify, String flag) {
        this.fileName = fileName;
        this.type = type.replaceAll(" ", "").toLowerCase();
        this.desc = desc;
        this.name = name;
        if (StringUtils.isNotBlank(verify)) {
            try {
                verifyConfig = JSON.parse(verify);
            } catch (Exception e) {
                String msg = String.format("验证信息格式错误 %s %s %s", fileName, name, verify);
                logger.error(msg);
                throw new ConfigException(msg);
            }
        } else if(configService != null) {
            verifyConfig = configService.getFieldVerifyDef(fileName, name);
        }
        if (flag != null) {
            this.flag = flag;
        }
    }

    public IFieldType getFieldType() {
        Class<? extends IFieldType> fieldTypeClass = ConfigFieldTypeFactory.getFieldTypeClass(type);
        IFieldType fieldType = null;
        try {
            fieldType = fieldTypeClass.newInstance();
        } catch (Exception e) {
            logger.error("IFieldType newInstance error " + type, e);
        }
        return fieldType;
    }

    public Object getVerifyConfig() {
        return verifyConfig;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getFileName() {
        return fileName;
    }
}
