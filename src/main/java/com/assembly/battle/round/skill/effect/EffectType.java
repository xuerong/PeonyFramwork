package com.assembly.battle.round.skill.effect;

import com.assembly.battle.BattleUtil;
import com.assembly.battle.round.skill.Skill;
import com.assembly.config.*;
import com.assembly.battle.round.skill.effect.effects.AddAttachEffect;
import com.assembly.battle.round.skill.effect.effects.AddBuffEffect;
import com.assembly.battle.round.skill.effect.effects.ChangeBasicDataAdditionEffect;
import com.assembly.battle.round.skill.effect.effects.ChangeBasicDataEffect;
import com.peony.engine.framework.tool.idenum.EnumUtils;
import com.peony.engine.framework.tool.idenum.IDEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能效果的分类
 *
 * @Author: zhengyuzhen
 * @Date: 2019-09-15 14:26
 */
public enum EffectType implements IDEnum,EffectCreator{
    ChangeBasicData(1,ChangeBasicDataEffect.class){
        @Override
        public Effect create(Skill skill,int effectId) {
            ChangeDataEffectConfig config = BattleUtil.configService.getItem(ChangeDataEffectContainer.class,effectId);
            ChangeBasicDataEffect changeBasicDataEffect =
                    new ChangeBasicDataEffect(skill,config);
            return changeBasicDataEffect;
        }
    },
    AddBuff(2, AddBuffEffect.class) {
        @Override
        public Effect create(Skill skill,int effectId) {
            AddBuffEffect addBuffEffect = new AddBuffEffect(skill,BattleUtil.configService.getItem(AddBuffEffectContainer.class,effectId));
            return addBuffEffect;
        }
    },
    AddAttach(3, AddAttachEffect.class) {
        @Override
        public Effect create(Skill skill,int effectId) {
            AddAttachEffect addAttachEffect = new AddAttachEffect(skill,BattleUtil.configService.getItem(AddAttachEffectContainer.class,effectId));
            return addAttachEffect;
        }
    },
    NoMagic(4, null) {
        @Override
        public Effect create(Skill skill,int effectId) {
            return null;
        }
    },
    ChangeBasicDataChange(5, ChangeBasicDataAdditionEffect.class) {
        @Override
        public Effect create(Skill skill,int effectId) {
            ChangeBasicDataAdditionEffect changeBasicDataAdditionEffect = new ChangeBasicDataAdditionEffect(skill,
                    BattleUtil.configService.getItem(ChangeDataChangeEffectContainer.class,effectId));
            return changeBasicDataAdditionEffect;
        }
    },
    ;
    final int id;
    final Class<? extends Effect> cls;

    EffectType(int id,Class<? extends Effect> cls){
        this.id = id;
        this.cls = cls;
    }

    public Class<? extends Effect> getCls() {
        return cls;
    }

    @Override
    public int getId() {
        return id;
    }

    public static EffectType valueOf(int id){
        return EnumUtils.getEnum(EffectType.class,id);
    }

    private static volatile Map<Class,EffectType> clsTypeMap ;
    public static EffectType getByCls(Class<?> cls){
        if(clsTypeMap == null){
            Map<Class,EffectType> clsTypeMap = new HashMap<>();
            for(EffectType effectType:EffectType.values()){
                clsTypeMap.put(effectType.cls,effectType);
            }
            EffectType.clsTypeMap = clsTypeMap;
        }
        return clsTypeMap.get(cls);
    }
}
interface EffectCreator{
    Effect create(Skill skill,int effectId);
}
