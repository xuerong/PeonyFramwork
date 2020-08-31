package com.peony.demo.myFruit.game.bag;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "bag",pks = {"uid","itemId"})
public class BagItem implements Serializable {
    private String uid;
    private int itemId;
    private int num;

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("uid",uid);
        ret.put("itemId",itemId);
        ret.put("num",num);
        return ret;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

}
