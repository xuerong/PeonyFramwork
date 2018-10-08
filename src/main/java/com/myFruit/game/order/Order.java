package com.myFruit.game.order;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.tool.util.Util;

import java.io.Serializable;
import java.util.Map;

@DBEntity(tableName = "userOrder",pks = {"uid","orderId"})
public class Order implements Serializable{
    private String uid;
    private int orderId;
    private int gold;
    private String items; // itemId,num

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("orderId",orderId);
        ret.put("gold",gold);
        Map<Integer,Integer> map = Util.split2Map(items,Integer.class,Integer.class);
        JSONArray array = new JSONArray();
        for(Map.Entry<Integer,Integer> entry : map.entrySet()){
            JSONObject item = new JSONObject();
            item.put("itemId",entry.getKey());
            item.put("num",entry.getValue());
            array.add(item);
        }
        ret.put("items",array);
        return ret;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }
}
