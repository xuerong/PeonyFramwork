package com.peony.platform.deploy;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;
@DBEntity(tableName = "deployserver",pks = {"projectId","id"})
public class DeployServer implements Serializable {
    private String projectId;
    private int id;
    private String name;

    private String path; // 部署的目录

    // ssh连接的时候的参数
    private String sshIp;
    private String sshUser;
    private String sshPassword;

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("projectId",projectId);
        ret.put("id",id);
        ret.put("name",name);
        ret.put("path",path);
        ret.put("sshIp",sshIp);
        ret.put("sshUser",sshUser);
        ret.put("sshPassword",sshPassword);
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


    public String getSshIp() {
        return sshIp;
    }

    public void setSshIp(String sshIp) {
        this.sshIp = sshIp;
    }

    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
