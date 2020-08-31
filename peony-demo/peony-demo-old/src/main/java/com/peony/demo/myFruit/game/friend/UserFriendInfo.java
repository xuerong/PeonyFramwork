package com.peony.demo.myFruit.game.friend;

import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

@DBEntity(tableName = "userFriendInfo",pks = {"uid"})
public class UserFriendInfo implements Serializable{
    private String uid;
    private int randomCount; // 随机玩家数量

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getRandomCount() {
        return randomCount;
    }

    public void setRandomCount(int randomCount) {
        this.randomCount = randomCount;
    }
}
