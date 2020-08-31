package com.peony.demo.myFruit.cmd.event.eventData;

import com.alibaba.fastjson.JSONObject;

public class LevelUpEventData {
    private String uid;
    private int fromLevel;
    private int toLevel;
    private JSONObject send2Client;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getFromLevel() {
        return fromLevel;
    }

    public void setFromLevel(int fromLevel) {
        this.fromLevel = fromLevel;
    }

    public int getToLevel() {
        return toLevel;
    }

    public void setToLevel(int toLevel) {
        this.toLevel = toLevel;
    }

    public JSONObject getSend2Client() {
        return send2Client;
    }

    public void setSend2Client(JSONObject send2Client) {
        this.send2Client = send2Client;
    }
}
