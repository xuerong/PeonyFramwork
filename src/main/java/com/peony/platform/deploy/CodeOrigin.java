package com.peony.platform.deploy;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.data.persistence.orm.annotation.Column;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.persistence.orm.annotation.StringTypeCollation;

import java.io.Serializable;


@DBEntity(tableName = "codeOrigin",pks = {"projectId","id"})
public class CodeOrigin implements Serializable{
    private String projectId;
    private int id;
    private String name;
    private int type; // 1本地，2git，3svn
    @Column(stringColumnType = StringTypeCollation.Text)
    private String params;

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("projectId",projectId);
        ret.put("id",id);
        ret.put("name",name);
        ret.put("type",type);
        ret.put("params",JSONObject.toJSON(params));
        return ret;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
