package com.peony.demo.myFruit.game.friend;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.data.persistence.orm.annotation.Column;
import com.peony.core.data.persistence.orm.annotation.DBEntity;
import com.peony.core.data.persistence.orm.annotation.StringTypeCollation;

import java.io.Serializable;

@DBEntity(tableName = "userfriend",pks = {"uid","friendUid"})
public class UserFriend implements Serializable {
    private String uid;
    private String friendUid;

    private int level;
    private int wuxing;

    @Column(stringColumnType = StringTypeCollation.Varchar128_Mb4)
    private String name;
    @Column(stringColumnType = StringTypeCollation.Varchar255)
    private String icon;
    private int gender;

    ///

    private int energy;
    private long energyTime; // 能量刷新

    //
    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        ret.put("friendUid",friendUid);
        ret.put("level",level);
        ret.put("wuxing",wuxing);
        ret.put("name",name);
        ret.put("icon",icon);
        ret.put("gender",gender);
        ret.put("energy",energy);
        ret.put("energyTime",energyTime);
        return ret;
    }



    //
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFriendUid() {
        return friendUid;
    }

    public void setFriendUid(String friendUid) {
        this.friendUid = friendUid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getWuxing() {
        return wuxing;
    }

    public void setWuxing(int wuxing) {
        this.wuxing = wuxing;
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

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public long getEnergyTime() {
        return energyTime;
    }

    public void setEnergyTime(long energyTime) {
        this.energyTime = energyTime;
    }
}
