package com.assembly.battle.round.skill.effect;

import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.skill.Skill;

import java.util.List;

/**
 *
 * 技能的效果
 *
 * @Author: zhengyuzhen
 * @Date: 2019-09-15 14:25
 */
public abstract class Effect {

    public Skill skill;

    public List<Effect> attachEffectList; // 绑定在上面的效果

    public Effect(Skill skill){
        this.skill = skill;
    }

    public void init(){ // 像加buf，之类的，触发的时

    }

    /**
     * 返回，技能作用到了谁
     * @param oriBattleUnit
     * @param roundBattle
     * @param skillTarget
     * @return
     */
    public abstract List<BattleUnit> releaseSkill(BattleUnit oriBattleUnit, RoundBattle roundBattle, List<BattleUnit> skillTarget);

    public EffectType getEffectType(){
        return EffectType.getByCls(this.getClass());
    }

    // TODO 这个地方得优化
    public abstract int getId();
}
