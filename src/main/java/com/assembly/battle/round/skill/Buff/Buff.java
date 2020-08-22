package com.assembly.battle.round.skill.Buff;

import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.skill.effect.EffectType;

import java.util.List;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-24 09:31
 */
public abstract class Buff {
    public BattleUnit fromBattleUnit;
    public int endRount;
    public Object[] params;


    public Buff(){}
    public Buff(Object[] params){
        this.params = params;
    }

    public boolean onAddToHero(BattleUnit oriBattleUnit,RoundBattle roundBattle){
        return true;
    }

    public abstract void releaseSkill(BattleUnit oriBattleUnit, RoundBattle roundBattle);

    public int endRound(){ // 结束回合
        return endRount;
    }

    public abstract void doRemove(BattleUnit oriBattleUnit, RoundBattle roundBattle);

    // TODO 能优化之吗
    public abstract int getId();

    public abstract List<Integer> getTags();

    public BuffType getBuffType(){
        return BuffType.getByCls(this.getClass());
    }
}
