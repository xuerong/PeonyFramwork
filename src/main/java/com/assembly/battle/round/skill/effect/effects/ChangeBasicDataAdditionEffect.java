package com.assembly.battle.round.skill.effect.effects;

import com.assembly.battle.BattleUtil;
import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.skill.Skill;
import com.assembly.battle.round.skill.TargetType;
import com.assembly.battle.round.skill.effect.Effect;
import com.assembly.battle.round.skill.effect.EffectType;
import com.assembly.config.ChangeDataChangeEffectConfig;
import com.peony.engine.config.core.Tuple3;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-26 07:52
 */
@Slf4j
public class ChangeBasicDataAdditionEffect extends Effect {
    ChangeDataChangeEffectConfig config;

    public ChangeBasicDataAdditionEffect(Skill skill,ChangeDataChangeEffectConfig config){
        super(skill);
        this.config =  config;
    }

    @Override
    public List<BattleUnit> releaseSkill(BattleUnit oriBattleUnit, RoundBattle roundBattle, List<BattleUnit> skillTarget) {
        // 获取目标
        Tuple3<Integer,Integer,Integer> tuple3 =  config.getTargetType();

        List<BattleUnit> battleUnitList = TargetType.valueOf(tuple3.getFirst()).getTarget(oriBattleUnit,roundBattle,skillTarget,tuple3.getSecond(),tuple3.getThird());

        List<Effect> effectList = BattleUtil.getEffectListByNavigation(battleUnitList,config.getTargetSkillId(),EffectType.ChangeBasicData
                ,config.getTargetEffectId(),((battleUnit, effect) -> {
                    ChangeBasicDataEffect changeBasicDataEffect = (ChangeBasicDataEffect)effect;
                    // 加上去
                    changeBasicDataEffect.changeDataChangeEffectConfigList.add(config);
                    log.info("技能参数加成[{}],{}[{}]->{}[{}]\ttarget:{},type:{},value:{},剩余{}" ,config.getName(),oriBattleUnit.getCamp(),oriBattleUnit.key(),battleUnit.getCamp(),
                            battleUnit.key(),config.getNewTargetType(),config.getChangeType(),config.getChangeValue(),changeBasicDataEffect.getFinalValue());
                }));
        return battleUnitList;
    }

    @Override
    public int getId() {
        return config.getId();
    }
}
