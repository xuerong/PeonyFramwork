package com.peony.core.server;

import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/11/13.
 */
@DBEntity(tableName = "idgenerator",pks = {"className"})
public class IdGenerator implements Serializable {
    private String className;
    private long id;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
