package com.myFruit.game.userBase;

import com.peony.engine.framework.tool.idenum.EnumUtils;
import com.peony.engine.framework.tool.idenum.IDEnum;

public enum  ShuXiang implements IDEnum{

    Jin(1,"金哥@💰",3,5,2,4,"sprite/items/zuowu_ningmeng"),
    Mu(2,"小木@🌲",3,4,1,5,"sprite/items/zuowu_yumi"),
    Shui(3,"大水@💧",1,2,4,5,"sprite/items/zuowu_lanmei"),
    Huo(4,"火兄@🔥",2,5,1,3,"sprite/items/zuowu_caomei"),
    Tu(5,"老土@🐰",1,4,3,2,"sprite/items/zuowu_nangua"),
    ;
    final int id;
    final String name;
    final int sheng1;
    final int sheng2;
    final int ke1;
    final int ke2;
    final String icon;
    ShuXiang(int id,String name,int sheng1,int sheng2,int ke1,int ke2,String icon){
        this.id = id;
        this.name = name;
        this.sheng1 = sheng1;
        this.sheng2 = sheng2;
        this.ke1 = ke1;
        this.ke2 = ke2;
        this.icon = icon;
    }

    public static ShuXiang valueOf(int value) {
        return EnumUtils.getEnumMap(ShuXiang.class).get(value);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSheng1() {
        return sheng1;
    }

    public int getSheng2() {
        return sheng2;
    }

    public int getKe1() {
        return ke1;
    }

    public int getKe2() {
        return ke2;
    }

    public String getIcon() {
        return icon;
    }
}
