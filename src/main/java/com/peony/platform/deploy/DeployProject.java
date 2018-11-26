package com.peony.platform.deploy;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.data.persistence.orm.annotation.Column;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.persistence.orm.annotation.StringTypeCollation;

import java.io.Serializable;

@DBEntity(tableName = "deployproject",pks = {"projectId"})
public class DeployProject implements Serializable{
    private String projectId;
    private String name;
    private String defaultSource;
    @Column(stringColumnType = StringTypeCollation.Text)
    private String sourceParam;

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("projectId",projectId);
        ret.put("name",name);
        ret.put("defaultSource",defaultSource);
        ret.put("sourceParam",sourceParam);
        return ret;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultSource() {
        return defaultSource;
    }

    public void setDefaultSource(String defaultSource) {
        this.defaultSource = defaultSource;
    }

    public String getSourceParam() {
        return sourceParam;
    }

    public void setSourceParam(String sourceParam) {
        this.sourceParam = sourceParam;
    }
}
