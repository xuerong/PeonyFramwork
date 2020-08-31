package com.peony.demo.assembly.battle.round.skill.effect.effects;

import com.peony.demo.assembly.battle.BattleUtil;
import com.peony.demo.assembly.battle.round.BattleUnit;
import com.peony.demo.assembly.battle.round.RoundBattle;
import com.peony.demo.assembly.battle.round.battleReport.BattleReport;
import com.peony.demo.assembly.battle.round.hero.BasicDataType;
import com.peony.demo.assembly.battle.round.hero.HeroUnit;
import com.peony.demo.assembly.battle.round.skill.Buff.Buff;
import com.peony.demo.assembly.battle.round.skill.Buff.BuffType;
import com.peony.demo.assembly.battle.round.skill.Skill;
import com.peony.demo.assembly.battle.round.skill.TargetType;
import com.peony.demo.assembly.battle.round.skill.effect.Effect;
import com.peony.demo.assembly.config.AddBuffEffectConfig;
import com.peony.demo.config.core.Tuple3;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-19 20:23
 */
@Slf4j
public class AddBuffEffect extends Effect {

    AddBuffEffectConfig addBuffEffectConfig;

    public AddBuffEffect(Skill skill,AddBuffEffectConfig addBuffEffectConfig){
        super(skill);
        this.addBuffEffectConfig = addBuffEffectConfig;
    }

    @Override
    public List<BattleUnit> releaseSkill(BattleUnit oriBattleUnit, RoundBattle roundBattle, List<BattleUnit> skillTarget) {
        Random random = new Random();
            //

        Tuple3<Integer,Integer,Integer> tuple3 =  addBuffEffectConfig.getTargetType();

        List<BattleUnit> battleUnitList = TargetType.valueOf(tuple3.getFirst()).getTarget(oriBattleUnit,roundBattle,skillTarget,tuple3.getSecond(),tuple3.getThird());

        List<BattleUnit> ret = new ArrayList<>();

        for(BattleUnit battleUnit:battleUnitList){
            if(random.nextInt(10000)< addBuffEffectConfig.getRate()){
                boolean isDodge = false;
                if(addBuffEffectConfig.isCanDodged()){
                    // 可以被闪避的话，要根据对方的闪避，和自己的命中确定是否闪避
                    long dodge = battleUnit.basicData.get(BasicDataType.Dodge);
                    if(dodge>0){
                        dodge -= oriBattleUnit.basicData.get(BasicDataType.HitRate);
                        if(dodge>0){
                            isDodge = BattleUtil.randomRatio(dodge);
                        }
                    }
                }
                if(!isDodge){
                    boolean exemptionControl = false;
                    if(BuffType.valueOf(addBuffEffectConfig.getBuffType()).isControl()){
                        long exemptionControlRate = battleUnit.basicData.get(BasicDataType.ExemptionControlRate);
                        if(exemptionControlRate > 0){
                            exemptionControl = BattleUtil.randomRatio(exemptionControlRate);
                        }
                    }
                    if(!exemptionControl){
                        log.info("触发添加效果的技能[{}]：{}[{}]->{}[{}]", addBuffEffectConfig.getName(),oriBattleUnit.getCamp(),oriBattleUnit.key(),
                                battleUnit.getCamp(),battleUnit.key());
                        HeroUnit heroUnit = (HeroUnit)battleUnit;

                        for(Integer buffId:addBuffEffectConfig.getBuffIds()){
                            BuffType buffType = BuffType.valueOf(addBuffEffectConfig.getBuffType());
                            Buff buff = buffType.create(oriBattleUnit,roundBattle,
                                    buffId);
                            heroUnit.addBuff(buffType,buff,roundBattle);
                            // @BattleReport
                            BattleReport.putSkillOper(roundBattle.getRound(),roundBattle.curHeroKey,
                                    new BattleReport.SkillOperData(oriBattleUnit.key(),battleUnit.key(),true,buffType,buff.getId()));
                        }
                        ret.add(battleUnit);
                    }else{
                        log.info("触发添加效果的技能[{}]：被免控",addBuffEffectConfig.getName());
                    }
                }else{
                    log.info("触发添加效果的技能[{}]：被闪避",addBuffEffectConfig.getName());
                }
            }
        }
        return ret;
    }

    @Override
    public int getId() {
        return addBuffEffectConfig.getId();
    }

}
