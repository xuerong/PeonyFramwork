package com.appPacket;

import com.peony.engine.framework.data.persistence.orm.annotation.Column;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.persistence.orm.annotation.StringTypeCollation;

import java.io.Serializable;

@DBEntity(tableName = "TestDbEntity",pks = {"uid"})
public class TestDbEntity implements Serializable {
    private String uid;
    private String aaa;

    @Column(stringColumnType = StringTypeCollation.Text_Mb4)
    private String bbb;


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAaa() {
        return aaa;
    }

    public void setAaa(String aaa) {
        this.aaa = aaa;
    }

    public String getBbb() {
        return bbb;
    }

    public void setBbb(String bbb) {
        this.bbb = bbb;
    }

}
