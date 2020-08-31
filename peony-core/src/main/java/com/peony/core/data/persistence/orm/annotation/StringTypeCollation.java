package com.peony.core.data.persistence.orm.annotation;
// 列的类型，因为String可能对应多中列的类型，可以生命之
public enum StringTypeCollation {
    Varchar128("varchar(128)","varchar(128)","utf8_general_ci"), // default
    Varchar128_Mb4("varchar(128) character set utf8mb4 COLLATE utf8mb4_unicode_ci","varchar(128)","utf8mb4_unicode_ci"), // 支持emoji表情符
    Varchar255("varchar(255)","varchar(255)","utf8_general_ci"),
    Text("text","text","utf8_general_ci"),
    Text_Mb4("text character set utf8mb4 COLLATE utf8mb4_unicode_ci","text","utf8mb4_unicode_ci"), // 支持emoji表情符
    ;
    final String des;
    final String typeDes;
    final String collation;
    StringTypeCollation(String des,String typeDes,String collation){
        this.des = des;
        this.typeDes = typeDes;
        this.collation = collation;
    }

    public String getDes() {
        return des;
    }

    public String getTypeDes() {
        return typeDes;
    }

    public String getCollation() {
        return collation;
    }
}
