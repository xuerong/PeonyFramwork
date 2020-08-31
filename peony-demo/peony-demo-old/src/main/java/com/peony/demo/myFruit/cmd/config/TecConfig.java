package com.peony.demo.myFruit.cmd.config;

import com.peony.demo.myFruit.game.tec.TecType;
import com.peony.common.exception.MMException;

public class TecConfig {



    public static TecConfig getTecConfig(TecType type, int level){
        switch (type){
            case ZengChan: // 增产
                return new TecConfig(level-1,level*5,level*2666); //{value:(level-1),upLevel:level*5,upGold:level*1666};
            case JiaSu: // 加速
                return new TecConfig((level-1)*5,level*3,level*1666); //{value:(level-1)*5,upLevel:level*6,upGold:level*1666};
            case YouYi: // 友谊
                return new TecConfig((level-1)*5,level*6,level*1266); //{value:(level-1)*5,upLevel:level*10,upGold:level*1666};
            case TanPan: // 谈判
                return new TecConfig((level-1)*5,level*4,level*3666); //{value:(level-1)*5,upLevel:level*8,upGold:level*1666};
        }
        throw new MMException("TecType error");
    }


    private int value;
    private int upgradeLevel;
    private int upgradeGold;

    public TecConfig(int value,int upgradeLevel,int upgradeGold){
        this.value = value;
        this.upgradeLevel = upgradeLevel;
        this.upgradeGold = upgradeGold;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    public int getUpgradeGold() {
        return upgradeGold;
    }

    public void setUpgradeGold(int upgradeGold) {
        this.upgradeGold = upgradeGold;
    }
}


