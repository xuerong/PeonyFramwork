package com.myFruit.game.tec;

import com.peony.engine.framework.tool.idenum.EnumUtils;
import com.peony.engine.framework.tool.idenum.IDEnum;

public enum TecType implements IDEnum {
    ZengChan(1),
    JiaSu(2),
    YouYi(3),
    TanPan(4),
    ;
    final int id;
    TecType(int id){
        this.id = id;
    }
    public final int getId() {
        return id;
    }
    public static TecType valueOf(int value) {
        return EnumUtils.getEnumMap(TecType.class).get(value);
    }
}
