package com.assembly.battle.round.hero;

import com.assembly.battle.BattleUtil;
import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.battleReport.BattleReport;
import com.assembly.battle.round.skill.Buff.Buff;
import com.assembly.battle.round.skill.Buff.BuffType;
import com.assembly.battle.round.skill.Skill;
import com.assembly.battle.round.skill.TriggerTime;
import com.assembly.battle.round.skill.effect.EffectType;
import com.assembly.config.HeroConfig;
import com.assembly.config.SkillConfig;
import com.assembly.config.SkillContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-15 14:22
 */
@Slf4j
public class HeroUnit extends BattleUnit {
   public HeroConfig heroConfig;

    String key;

    boolean fightAgain = false; // 本回合，本次攻击之后，是否再次攻击

    public HeroUnit(int camp, String key, HeroConfig heroConfig){

        Map<BasicDataType,Long> values = new HashMap<>();
        for(Map.Entry<Integer,Long> entry:heroConfig.getBasicData().entrySet()){
            values.put(BasicDataType.valueOf(entry.getKey()),entry.getValue());
        }

        BasicData basicData = new BasicData(values);

        List<Skill> skillList = new ArrayList<>();
        for(Integer skillId:heroConfig.getSkillIdList()){
            SkillConfig skillConfig = BattleUtil.configService.getItem(SkillContainer.class,skillId);//SkillConfig.get(skillId);
            Skill skill = new Skill(skillConfig);
            skillList.add(skill);
        }
        init(key,basicData,skillList,camp);
        this.heroConfig = heroConfig;
    }

    private void init(String key,BasicData basicData,List<Skill> skillList,int camp){
        this.key = key;
        this.basicData = basicData;
        this.skillList = skillList;
        this.camp = camp;
        for(Skill skill:skillList){
            skill.setHeroUnit(this);
        }
    }

    public void addBuff(BuffType buffType, Buff buff, RoundBattle roundBattle){
        if(buff.onAddToHero(this,roundBattle)){
            List<Buff> effectList = buffTypeListMap.get(buffType);
            if(effectList == null){
                effectList = new ArrayList<>();
                buffTypeListMap.put(buffType,effectList);
            }
            effectList.add(buff);
        }
    }

    @Override
    public void roundBegin() {
        //
        List<Buff> hardControlEffectList = buffTypeListMap.get(BuffType.ChangeBasicData);
        if(CollectionUtils.isNotEmpty(hardControlEffectList)){
            for(Buff buff:hardControlEffectList){
                buff.releaseSkill(this,roundBattle);
            }
        }
    }

    @Override
    public boolean fight() {
        /**
         * 是否硬控
         * 是否释放魔法（释放方式，禁魔）
         * 释放技能
         */

        // 是否硬控
        List<Buff> hardControlEffectList = buffTypeListMap.get(BuffType.HardControl);
        if(CollectionUtils.isNotEmpty(hardControlEffectList)){
            log.info("{} 被硬控",this.key);
            return fightAgain;
        }
        // 要执行的技能
        List<Skill> execSkillList = new ArrayList<>();

        // 是否释放魔法
        // 是否有：魔法到了就释放的技能
        long allUseMagicValue = 0;
        List<Buff> noMagicEffectList = buffTypeListMap.get(EffectType.NoMagic);
        if(CollectionUtils.isEmpty(noMagicEffectList)){
            // 没有被禁魔
            long magic = basicData.get(BasicDataType.Magic);
            List<Skill> skillList = skillTriggerTimes.get(TriggerTime.Fight_Magic_Enough);
            if(CollectionUtils.isNotEmpty(skillList)){
                for(Skill skill:skillList){
                    if(skill.useMagicValue() + allUseMagicValue <= magic){
                        execSkillList.add(skill);
                        allUseMagicValue += skill.useMagicValue();
                    }
                }
            }

        }
        if(execSkillList.size() == 0){
            execSkillList = skillTriggerTimes.get(TriggerTime.Fight_Default); // skillList.get(defaultSkillIndex);
        }

        // 扣除魔法
        if(allUseMagicValue > 0){
            basicData.changeBasicData(BasicDataType.Magic,-allUseMagicValue);
        }
        // 释放技能
        for(Skill skill:execSkillList) {
            skill.releaseSkill(roundBattle);
        }
        //
        return fightAgain;
    }

    @Override
    public void roundEnd() {
        // 移除过期效果
        Iterator<List<Buff>> buffListIterator = buffTypeListMap.values().iterator();
        while (buffListIterator.hasNext()){
            List<Buff> buffList = buffListIterator.next();
            Iterator<Buff> effectIterator = buffList.iterator();
            while (effectIterator.hasNext()){
                Buff buff = effectIterator.next();
                if(buff.endRound() <= roundBattle.getRound()){
                    buff.doRemove(this,roundBattle);
                    effectIterator.remove();
                    // @BattleReport
                    BattleReport.putSkillOper(roundBattle.getRound(),roundBattle.curHeroKey,new BattleReport.SkillOperData(
                            buff.fromBattleUnit.key(),this.key,false,buff.getBuffType(),buff.getId()
                    ));
                }
            }
            if(buffList.size() == 0){
                buffListIterator.remove();
            }
        }
        //
    }

    @Override
    public boolean alive() {
        return basicData.get(BasicDataType.Blood) > 0;
    }

    @Override
    public int belongToTroop() {
        return 0;
    }

    @Override
    public long speed() {
        return basicData.get(BasicDataType.Speed);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public int compareTo(BattleUnit o) {
        long a = this.speed()-o.speed();
        long b = this.getCamp() - o.getCamp();
        return a>0?-1:((a==0)?(b>0?1:((b==0)?0:-1)):1);
    }
}
