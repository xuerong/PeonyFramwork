package com.myFruit.game.task;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 *
 * 每天会重置任务，任务分成7中：
 * 3个水果：5,10,20,40，每次是上次的2倍
 * 一个订单：4,8，每次是上次的2倍
 * 使用肥料：5,10,20，每次是上次的2倍
 * 加速
 * 升级
 *
 */
@DBEntity(tableName = "usertask",pks = {"uid"})
public class UserTask implements Serializable {
    private String uid;
    private long refreshTime;

    private int awardDaily; // 上次领取每日奖励时间

    private int allFruit; // 所有水果数量
    private int allFruitCur; // 所有水果数量

    private int fruitType; // 单个水果类型
    private int fruit; // 单个水果数量
    private int fruitCur; // 单个水果数量

    private int order;
    private int orderCur;

    private int speedUp;
    private int speedUpCur;

    private int fertilizer;
    private int fertilizerCur;


    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("refreshTime",refreshTime);
        ret.put("awardDaily",awardDaily);

        ret.put("allFruit",allFruit);
        ret.put("allFruitCur",allFruitCur);

        ret.put("fruitType",fruitType);
        ret.put("fruit",fruit);
        ret.put("fruitCur",fruitCur);
        ret.put("order",order);
        ret.put("orderCur",orderCur);
        ret.put("speedUp",speedUp);
        ret.put("speedUpCur",speedUpCur);
        ret.put("fertilizer",fertilizer);
        ret.put("fertilizerCur",fertilizerCur);
        return ret;
    }



    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getRefreshTime() {
        return refreshTime;
    }

    public void setRefreshTime(long refreshTime) {
        this.refreshTime = refreshTime;
    }

    public int getAwardDaily() {
        return awardDaily;
    }

    public void setAwardDaily(int awardDaily) {
        this.awardDaily = awardDaily;
    }

    public int getAllFruit() {
        return allFruit;
    }

    public void setAllFruit(int allFruit) {
        this.allFruit = allFruit;
    }

    public int getFruitType() {
        return fruitType;
    }

    public void setFruitType(int fruitType) {
        this.fruitType = fruitType;
    }

    public int getFruit() {
        return fruit;
    }

    public void setFruit(int fruit) {
        this.fruit = fruit;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getSpeedUp() {
        return speedUp;
    }

    public void setSpeedUp(int speedUp) {
        this.speedUp = speedUp;
    }

    public int getFertilizer() {
        return fertilizer;
    }

    public void setFertilizer(int fertilizer) {
        this.fertilizer = fertilizer;
    }

    public int getAllFruitCur() {
        return allFruitCur;
    }

    public void setAllFruitCur(int allFruitCur) {
        this.allFruitCur = allFruitCur;
    }

    public int getFruitCur() {
        return fruitCur;
    }

    public void setFruitCur(int fruitCur) {
        this.fruitCur = fruitCur;
    }

    public int getOrderCur() {
        return orderCur;
    }

    public void setOrderCur(int orderCur) {
        this.orderCur = orderCur;
    }

    public int getSpeedUpCur() {
        return speedUpCur;
    }

    public void setSpeedUpCur(int speedUpCur) {
        this.speedUpCur = speedUpCur;
    }

    public int getFertilizerCur() {
        return fertilizerCur;
    }

    public void setFertilizerCur(int fertilizerCur) {
        this.fertilizerCur = fertilizerCur;
    }
}
