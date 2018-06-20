package com.peony.engine.config.core;

import com.google.api.client.util.Maps;

import java.util.Map;

/**
 * Created by jiangmin.wu on 17/7/19.
 */
public class ConfigMetaData {
    private String fileName;
    private String orignFileName;
    private Map<String, ConfigFieldMeta> fields = Maps.newHashMap();

    public String getOrignFileName() {
        return orignFileName;
    }

    public void setOrignFileName(String orignFileName) {
        this.orignFileName = orignFileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, ConfigFieldMeta> getFields() {
        return fields;
    }

    public void setFields(Map<String, ConfigFieldMeta> fields) {
        this.fields = fields;
    }
}
