package com.peony.demo.myFruit.game.skill;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "userskill",pks = {"uid"})
public class UserSkill implements Serializable{
    private String uid;
    private int fertilizer; // 肥料数量
    private int speedPower; // 加速数量

    private long refreshTime; // 每天的刷新时间

    private long speedBeginTime; // 加速开始时间
    private long speedEndTime; // 加速结束时间


    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("fertilizer",fertilizer);
        ret.put("speedPower",speedPower);
        ret.put("refreshTime",refreshTime);
        ret.put("speedBeginTime",speedBeginTime);
        ret.put("speedEndTime",speedEndTime);
        return ret;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getFertilizer() {
        return fertilizer;
    }

    public void setFertilizer(int fertilizer) {
        this.fertilizer = fertilizer;
    }

    public int getSpeedPower() {
        return speedPower;
    }

    public void setSpeedPower(int speedPower) {
        this.speedPower = speedPower;
    }

    public long getRefreshTime() {
        return refreshTime;
    }

    public void setRefreshTime(long refreshTime) {
        this.refreshTime = refreshTime;
    }

    public long getSpeedBeginTime() {
        return speedBeginTime;
    }

    public void setSpeedBeginTime(long speedBeginTime) {
        this.speedBeginTime = speedBeginTime;
    }

    public long getSpeedEndTime() {
        return speedEndTime;
    }

    public void setSpeedEndTime(long speedEndTime) {
        this.speedEndTime = speedEndTime;
    }
}
