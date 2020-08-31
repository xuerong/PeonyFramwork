package com.peony.demo.assembly.battle.round.skill;

import com.peony.common.tool.idenum.EnumUtils;
import com.peony.common.tool.idenum.IDEnum;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-25 12:03
 */
public enum TriggerTime implements IDEnum {
    Fight_Default(1),// 默认攻击，英雄
    Fight_Magic_Enough(2), // 魔法达到触发值，英雄
    Init(3), // 初始化，英雄
    Dead(4), // 英雄死亡时
    ;
    final int id;
    TriggerTime(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }
    public static TriggerTime valueOf(int id){
        return EnumUtils.getEnum(TriggerTime.class,id);
    }

}
