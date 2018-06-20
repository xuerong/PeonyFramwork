package com.peony.engine.framework.data.sysPara;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * Created by apple on 16-10-2.
 * 存储系统变量
 */
@DBEntity(tableName = "syspara",pks = {"id"})
public class SysPara implements Serializable {
    private String id;
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
