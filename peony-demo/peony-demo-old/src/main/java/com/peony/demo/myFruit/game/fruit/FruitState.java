package com.peony.demo.myFruit.game.fruit;

import com.peony.common.tool.idenum.EnumUtils;
import com.peony.common.tool.idenum.IDEnum;

public enum FruitState implements IDEnum{
    Locked (0) ,
    Idle (1),
    Growing (2),
    Mature (3)
    ;

    final int id;
    FruitState(int id){
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    public static FruitState valueOf(int value) {
        return EnumUtils.getEnumMap(FruitState.class).get(value);
    }
}
