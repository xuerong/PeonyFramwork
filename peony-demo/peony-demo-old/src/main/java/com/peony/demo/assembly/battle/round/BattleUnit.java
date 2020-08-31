package com.peony.demo.assembly.battle.round;

import com.peony.demo.assembly.battle.round.hero.BasicData;
import com.peony.demo.assembly.battle.round.skill.Buff.Buff;
import com.peony.demo.assembly.battle.round.skill.Buff.BuffType;
import com.peony.demo.assembly.battle.round.skill.Skill;
import com.peony.demo.assembly.battle.round.skill.TriggerTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-15 14:11
 */
@Slf4j
public abstract class BattleUnit implements Comparable<BattleUnit>{

    public BasicData basicData;

    public List<Skill> skillList; // 自己的技能列表
    protected RoundBattle roundBattle;
    protected int camp; // 属于哪个阵营，

    protected Map<TriggerTime,List<Skill>> skillTriggerTimes = new HashMap<>(); // 自己在每个节点会触发的技能（可能是别人的技能）

    // 英雄身上的效果
    public Map<BuffType, List<Buff>> buffTypeListMap = new HashMap<>();

    public void setRoundBattle(RoundBattle roundBattle) {
        this.roundBattle = roundBattle;
    }

    protected abstract void roundBegin();
    protected abstract boolean fight();
    protected abstract void roundEnd();
    public abstract boolean alive();
    protected void onDead(){
        log.info("英雄死亡：{},{}",camp,key());
        List<Skill> deadSkillList = skillTriggerTimes.get(TriggerTime.Dead);
        if(CollectionUtils.isNotEmpty(deadSkillList)){
            for(Skill skill:deadSkillList){
                skill.releaseSkill(roundBattle);
            }
        }
    }

    public int getCamp(){
        return camp;
    }
    protected abstract int belongToTroop(); // 属于哪个军队

    public abstract long speed();

    public abstract String key();


    public void init(){
        List<Skill> list = skillTriggerTimes.get(TriggerTime.Init);
        if(CollectionUtils.isNotEmpty(list)){
            for(Skill skill:list){
                skill.releaseSkill(roundBattle);
            }
        }
    }

    public void addSkillTriggerTime(TriggerTime triggerTime,Skill skill){
        List<Skill> list = skillTriggerTimes.get(triggerTime);
        if(list == null){
            list = new ArrayList<>();
            skillTriggerTimes.put(triggerTime,list);
        }
        list.add(skill);
    }
    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        }
        if(o == this){
            return true;
        }

        BattleUnit battleUnit = (BattleUnit)o;

        return battleUnit.key().equals(this.key());
    }
}
