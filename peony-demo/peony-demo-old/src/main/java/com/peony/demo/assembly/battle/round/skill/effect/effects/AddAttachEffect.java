package com.peony.demo.assembly.battle.round.skill.effect.effects;

import com.peony.demo.assembly.battle.BattleUtil;
import com.peony.demo.assembly.battle.round.BattleUnit;
import com.peony.demo.assembly.battle.round.RoundBattle;
import com.peony.demo.assembly.battle.round.skill.Skill;
import com.peony.demo.assembly.battle.round.skill.TargetType;
import com.peony.demo.assembly.battle.round.skill.effect.Effect;
import com.peony.demo.assembly.battle.round.skill.effect.EffectType;
import com.peony.demo.assembly.config.AddAttachEffectConfig;
import com.google.common.collect.ImmutableList;
import com.peony.demo.config.core.Tuple3;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-25 20:06
 */
@Slf4j
public class AddAttachEffect extends Effect {

    AddAttachEffectConfig config;
    public AddAttachEffect(Skill skill,AddAttachEffectConfig config){
        super(skill);
        this.config = config;
    }


    @Override
    public List<BattleUnit> releaseSkill(BattleUnit oriBattleUnit, RoundBattle roundBattle, List<BattleUnit> skillTarget) {
        // 获取目标
        Tuple3<Integer,Integer,Integer> tuple3 =  config.getTargetType();

        List<BattleUnit> battleUnitList = TargetType.valueOf(tuple3.getFirst()).getTarget(oriBattleUnit,roundBattle,skillTarget,tuple3.getSecond(),tuple3.getThird());

        List<Effect> effectList = BattleUtil.getEffectListByNavigation(battleUnitList,config.getTargetSkillId(),EffectType.valueOf(config.getTargetEffectType())
                ,config.getTargetEffectId(),((battleUnit, effect) -> {
                    // 加上去
                    for(Map.Entry<Integer, ImmutableList<Integer>> entry:config.getAttachEffectIdListMap().entrySet()){
                        for(Integer effectId:entry.getValue()){
                            log.info("添加技能绑定技能[{}],{}[{}]->{}[{}]",config.getName(),oriBattleUnit.getCamp(),oriBattleUnit.key(),battleUnit.getCamp(),battleUnit.key());
                            effect.attachEffectList.add(EffectType.valueOf(entry.getKey()).create(skill,effectId));
                        }
                    }
                }));
        return battleUnitList;
    }

    @Override
    public int getId() {
        return config.getId();
    }
}
