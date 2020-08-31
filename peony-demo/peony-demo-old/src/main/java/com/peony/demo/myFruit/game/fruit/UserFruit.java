package com.peony.demo.myFruit.game.fruit;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "userfruit",pks = {"uid","posId"})
public class UserFruit implements Serializable {
    private String uid;
    private int posId;
    // state,fruitData.itemId,fruitData.fruitNum,fruitData.finishTime
    /**
     * Locked = 0,
     Idle = 1,
     Growing = 2,
     Mature = 3
     */
    private int state;
    private int itemId;
    private int fruitNum;
    private long beginTime;
    private long finishTime;
    private int fertilizer;



    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getPosId() {
        return posId;
    }

    public void setPosId(int posId) {
        this.posId = posId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getFruitNum() {
        return fruitNum;
    }

    public void setFruitNum(int fruitNum) {
        this.fruitNum = fruitNum;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public int getFertilizer() {
        return fertilizer;
    }

    public void setFertilizer(int fertilizer) {
        this.fertilizer = fertilizer;
    }
}
