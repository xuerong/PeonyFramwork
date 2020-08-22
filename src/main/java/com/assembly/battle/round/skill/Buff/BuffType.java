package com.assembly.battle.round.skill.Buff;

import com.assembly.battle.BattleUtil;
import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.skill.Buff.buff.ChangeBasicDataBuff;
import com.assembly.battle.round.skill.Buff.buff.HardControlBuff;
import com.assembly.battle.round.skill.effect.EffectType;
import com.assembly.config.ChangeDataBuffContainer;
import com.assembly.config.HardControlBuffContainer;
import com.peony.engine.framework.tool.idenum.EnumUtils;
import com.peony.engine.framework.tool.idenum.IDEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-24 09:35
 */
public enum BuffType implements BuffCreator, IDEnum {
    ChangeBasicData(1,false, ChangeBasicDataBuff.class) {
        @Override
        public Buff create(BattleUnit oriBattleUnit, RoundBattle roundBattle, Object... params) {
            ChangeBasicDataBuff changeBasicDataBuff = new ChangeBasicDataBuff();

            int id = (Integer)params[0];
            changeBasicDataBuff.fromBattleUnit = oriBattleUnit;
            changeBasicDataBuff.changeDataBuffConfig = BattleUtil.configService.getItem(ChangeDataBuffContainer.class,id);
            changeBasicDataBuff.endRount = roundBattle.getRound() + changeBasicDataBuff.changeDataBuffConfig.getRound() - 1;
            return changeBasicDataBuff;
        }
    },
    HardControl(2,true, HardControlBuff.class){
        @Override
        public Buff create(BattleUnit oriBattleUnit, RoundBattle roundBattle,Object... params) {
            HardControlBuff hardControlBuff = new HardControlBuff();
            int id = (Integer)params[0];
            hardControlBuff.fromBattleUnit = oriBattleUnit;
            hardControlBuff.hardControlBuffConfig = BattleUtil.configService.getItem(HardControlBuffContainer.class,id);
            hardControlBuff.endRount = roundBattle.getRound() + hardControlBuff.hardControlBuffConfig.getRound() - 1;
            return hardControlBuff;
        }
    },
    ;
    final int id;
    final boolean control; // 控制技能
    final Class<? extends Buff> cls;

    BuffType(int id,boolean control,Class<? extends Buff> cls){
        this.id = id;
        this.control = control;
        this.cls = cls;
    }

    public Class<? extends Buff> getCls() {
        return cls;
    }

    @Override
    public int getId() {
        return id;
    }

    public boolean isControl() {
        return control;
    }

    public static BuffType valueOf(int id){
        return EnumUtils.getEnum(BuffType.class,id);
    }


    private static volatile Map<Class, BuffType> clsTypeMap ;
    public static BuffType getByCls(Class<?> cls){
        if(clsTypeMap == null){
            Map<Class,BuffType> clsTypeMap = new HashMap<>();
            for(BuffType buffType:BuffType.values()){
                clsTypeMap.put(buffType.cls,buffType);
            }
            BuffType.clsTypeMap = clsTypeMap;
        }
        return clsTypeMap.get(cls);
    }
}
interface BuffCreator{
    Buff create(BattleUnit oriBattleUnit, RoundBattle roundBattle,Object... params);
}