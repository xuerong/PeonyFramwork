package com.peony.engine.framework.data.persistence.dao;

/**
 * Created by a on 2016/9/21.
 */
public class ColumnDesc {
    private String field;
    private String type;
    private String collation;
    private boolean key;
    private boolean defaultNull;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }

    public boolean isKey() {
        return key;
    }

    public void setKey(boolean key) {
        this.key = key;
    }

    public boolean isDefaultNull() {
        return defaultNull;
    }

    public void setDefaultNull(boolean defaultNull) {
        this.defaultNull = defaultNull;
    }
}
