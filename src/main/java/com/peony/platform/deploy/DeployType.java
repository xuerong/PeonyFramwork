package com.peony.platform.deploy;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.data.persistence.orm.annotation.Column;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.persistence.orm.annotation.StringTypeCollation;

import java.io.Serializable;

@DBEntity(tableName = "deploytype",pks = {"projectId","id"})
public class DeployType implements Serializable{
    private String projectId;
    private String id;
    private String name;
    private int codeOrigin;
    private String env;
    private String buildParams;
    private int restart;
    @Column(stringColumnType = StringTypeCollation.Text)
    private String serverIds; // 该部署对应的serverIds

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("id",id);
        ret.put("name",name);
        ret.put("codeOrigin", codeOrigin);
        ret.put("env",env);
        ret.put("buildParams",buildParams);
        ret.put("restart",restart);
        ret.put("serverIds",serverIds);
        return ret;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCodeOrigin() {
        return codeOrigin;
    }

    public void setCodeOrigin(int codeOrigin) {
        this.codeOrigin = codeOrigin;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getBuildParams() {
        return buildParams;
    }

    public void setBuildParams(String buildParams) {
        this.buildParams = buildParams;
    }

    public int getRestart() {
        return restart;
    }

    public void setRestart(int restart) {
        this.restart = restart;
    }

    public String getServerIds() {
        return serverIds;
    }

    public void setServerIds(String serverIds) {
        this.serverIds = serverIds;
    }
}
