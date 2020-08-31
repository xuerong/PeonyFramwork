package com.peony.core.control.statistics;

import com.peony.core.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * 存储统计信息，如果需要
 */
@DBEntity(tableName = "statisticsstore",pks = {"id"})
public class StatisticsStore implements Serializable{
    private long id;
    private String type;
    private String content;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
