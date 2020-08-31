package com.peony.demo.myFruit.game.tec;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "technology",pks = {"uid"})
public class Technology implements Serializable{
    private String uid;
    private int zengchan;
    private int jiasu;
    private int youyi;
    private int tanpan;

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("zengchan",zengchan);
        ret.put("jiasu",jiasu);
        ret.put("youyi",youyi);
        ret.put("tanpan",tanpan);
        return ret;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getZengchan() {
        return zengchan;
    }

    public void setZengchan(int zengchan) {
        this.zengchan = zengchan;
    }

    public int getJiasu() {
        return jiasu;
    }

    public void setJiasu(int jiasu) {
        this.jiasu = jiasu;
    }

    public int getYouyi() {
        return youyi;
    }

    public void setYouyi(int youyi) {
        this.youyi = youyi;
    }

    public int getTanpan() {
        return tanpan;
    }

    public void setTanpan(int tanpan) {
        this.tanpan = tanpan;
    }
}
