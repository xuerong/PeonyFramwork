package com.assembly.battle.round.skill;

import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.hero.HeroUnit;
import com.assembly.battle.round.skill.effect.Effect;
import com.assembly.battle.round.skill.effect.EffectType;
import com.assembly.config.SkillConfig;
import com.google.common.collect.ImmutableList;
import com.peony.engine.config.core.Tuple3;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-15 14:39
 */
@Slf4j
public class Skill {
    public SkillConfig skillConfig;

    // 技能带有的效果
    public List<Effect> effects;

    protected  HeroUnit heroUnit;

    public Skill(SkillConfig skillConfig){

        this.skillConfig = skillConfig;

        List<Effect> effects = new ArrayList<>();
        for(Map.Entry<Integer, ImmutableList<Integer>> entry:skillConfig.getEffectIdListMap().entrySet()){
            EffectType effectType = EffectType.valueOf(entry.getKey());
            for(Integer effectId:entry.getValue()){
                effects.add(effectType.create(this,effectId));
            }
        }
        this.effects = effects;
    }

    public void setHeroUnit(HeroUnit heroUnit) {
        this.heroUnit = heroUnit;
    }

    public void init(){
        for(Effect effect:effects){
            effect.init();
        }
    }

    public boolean useMagic(){
        return useMagicValue()>0;
    }
    public long useMagicValue(){
        return skillConfig.getUseMagic();
    }

    public void releaseSkill(RoundBattle roundBattle){
        TriggerTime triggerTime = TriggerTime.valueOf(skillConfig.getTriggerTime());
        // 如果英雄死了就不能执行了。但是绑定者还绑定着他的效果呢，
        if(triggerTime != TriggerTime.Init && !skillConfig.isEnableWhileDead() && !heroUnit.alive()){
            return;
        }
        log.info("{} 释放技能：{}",heroUnit.key(), skillConfig.getName());
        Tuple3<Integer,Integer,Integer> tuple3 =  skillConfig.getTargetType();
        List<BattleUnit> skillTarget = TargetType.valueOf(tuple3.getFirst()).getTarget(heroUnit,roundBattle,null,tuple3.getSecond(),tuple3.getThird());
        for(Effect effect:effects){
            doRelease(effect,roundBattle,skillTarget);
        }
    }
    private void doRelease(Effect effect,RoundBattle roundBattle,List<BattleUnit> skillTarget){
        List<BattleUnit> battleUnitList = effect.releaseSkill(heroUnit,roundBattle,skillTarget);
        if(CollectionUtils.isNotEmpty(battleUnitList)){
            if (CollectionUtils.isNotEmpty(effect.attachEffectList)) {
                for(Effect attachEffect:effect.attachEffectList){
                    doRelease(attachEffect,roundBattle,battleUnitList);
                }
            }
        }
    }
}
