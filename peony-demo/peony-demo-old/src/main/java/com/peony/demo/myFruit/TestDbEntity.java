package com.peony.demo.myFruit;

import com.peony.core.data.persistence.orm.annotation.Column;
import com.peony.core.data.persistence.orm.annotation.DBEntity;
import com.peony.core.data.persistence.orm.annotation.StringTypeCollation;

import java.io.Serializable;

@DBEntity(tableName = "TestDbEntity",pks = {"uid","aaa"})
public class TestDbEntity implements Serializable {
    private String uid;
    private String aaa;

    @Column(stringColumnType = StringTypeCollation.Text_Mb4)
    private String ccc;


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

//    public String getBbb() {
//        return bbb;
//    }
//
//    public void setBbb(String bbb) {
//        this.bbb = bbb;
//    }


    public String getCcc() {
        return ccc;
    }

    public void setCcc(String ccc) {
        this.ccc = ccc;
    }

    public String toString(){
        return new StringBuilder("uid="+uid).append(",aaa=").append(aaa).append(",bbb=").append(ccc).toString();
    }

}
