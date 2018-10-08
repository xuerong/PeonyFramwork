package com.myFruit.game.userBase;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.data.persistence.orm.annotation.Column;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.persistence.orm.annotation.StringTypeCollation;

import java.io.Serializable;

@DBEntity(tableName = "userbase",pks = {"uid"})
public class UserBase implements Serializable{
    private String uid;
    private int level;
    private int exp;
    private int gold;
    //
    private int shuXiang;
    //
    @Column(stringColumnType = StringTypeCollation.Varchar128_Mb4)
    private String name;
    @Column(stringColumnType = StringTypeCollation.Varchar255)
    private String icon;
    private int gender;

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("uid",uid);
        ret.put("level",level);
        ret.put("exp",exp);
        ret.put("gold",gold);
        ret.put("shuXiang", shuXiang);
        ret.put("name",name);
        ret.put("icon",icon);
        ret.put("gender", gender);
        return ret;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public int getShuXiang() {
        return shuXiang;
    }

    public void setShuXiang(int shuXiang) {
        this.shuXiang = shuXiang;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }
}
