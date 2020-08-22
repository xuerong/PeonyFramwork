package com.assembly.battle.round.skill.Buff.buff;

import com.assembly.battle.BattleUtil;
import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.battleReport.BattleReport;
import com.assembly.battle.round.hero.BasicDataType;
import com.assembly.battle.round.skill.Buff.Buff;
import com.assembly.battle.round.skill.Buff.BuffType;
import com.assembly.config.ChangeDataBuffConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 *
 * 普攻
 *
 * @Author: zhengyuzhen
 * @Date: 2019-09-19 20:09
 */
@Slf4j
public class ChangeBasicDataBuff extends Buff {
    public ChangeDataBuffConfig changeDataBuffConfig;

    public long value; // 计算好的value存储

    private long changeValue = 0;

    @Override
    public boolean onAddToHero(BattleUnit oriBattleUnit,RoundBattle roundBattle){
        // 如果不是实时计算，先计算好，放好
        if(!changeDataBuffConfig.isCalWhenUse()){
            this.value = calChangeValue(oriBattleUnit);
        }
        // 对于消耗性的：需要扣除本回合，且如果回合数只有1，就不要加在对方身上
        trigger(oriBattleUnit,roundBattle);

        if(changeDataBuffConfig.isConsumptive()){
            return changeDataBuffConfig.getRound()>1;
        }
        return true;
    }


    @Override
    public void releaseSkill(BattleUnit oriBattleUnit,RoundBattle roundBattle) {
        if(changeDataBuffConfig.isConsumptive()){ // 消耗性的，继续触发，否则不再触发
            trigger(oriBattleUnit,roundBattle);
        }
    }

    private void trigger(BattleUnit oriBattleUnit,RoundBattle roundBattle){
        long changeValue;
        if(!changeDataBuffConfig.isCalWhenUse()){
            changeValue = this.value;
        }else{
            changeValue = calChangeValue(oriBattleUnit);
        }
        BasicDataType basicDataType = BasicDataType.valueOf(changeDataBuffConfig.getBasicDataType());
        long lastValue = oriBattleUnit.basicData.changeBasicData(basicDataType,changeValue);

        this.changeValue+=changeValue;
        log.info("{}[{}]->{}[{}]\t{}\t剩余{}",fromBattleUnit.getCamp(),fromBattleUnit.key(),oriBattleUnit.getCamp(),oriBattleUnit.key(),changeValue,lastValue);
        // @BattleReport
        BattleReport.putSkillOper(roundBattle.getRound(),roundBattle.curHeroKey
                ,new BattleReport.SkillOperData(fromBattleUnit.key(),oriBattleUnit.key(), BuffType.ChangeBasicData,changeDataBuffConfig.getId(),
                        basicDataType,changeValue, BattleReport.OperType.BuffRelease));
    }
    private long calChangeValue(BattleUnit oriBattleUnit){
        this.value = BattleUtil.calChangeBasicData(fromBattleUnit,oriBattleUnit, changeDataBuffConfig.getValueTypeId(), changeDataBuffConfig.getValue());
        if(changeDataBuffConfig.getBasicDataType() == BasicDataType.Blood.getId()){
            value = BattleUtil.calAttackValue(value,oriBattleUnit,oriBattleUnit, changeDataBuffConfig.isCanCrit(), changeDataBuffConfig.isCanDefend());
        }
        return value;
    }

    @Override
    public void doRemove(BattleUnit oriBattleUnit, RoundBattle roundBattle){
        if(!changeDataBuffConfig.isConsumptive()){
            // buff改回去
            BasicDataType basicDataType = BasicDataType.valueOf(changeDataBuffConfig.getBasicDataType());
            oriBattleUnit.basicData.changeBasicData(basicDataType,-this.changeValue);
            log.info("buf结束，{}[{}]->{}[{}]\t{}\t剩余{}",fromBattleUnit.getCamp(),fromBattleUnit.key(),oriBattleUnit.getCamp(),
                    oriBattleUnit.key(),-changeValue,oriBattleUnit.basicData.get(BasicDataType.valueOf(changeDataBuffConfig.getBasicDataType())));
            // @BattleReport
            BattleReport.putSkillOper(roundBattle.getRound(),roundBattle.curHeroKey,new BattleReport.SkillOperData(
                fromBattleUnit.key(),oriBattleUnit.key(),getBuffType(),getId(),basicDataType,-this.changeValue, BattleReport.OperType.DelBuffRelease
            ));
        }
    }

    @Override
    public int getId() {
        return changeDataBuffConfig.getId();
    }

    @Override
    public List<Integer> getTags() {
        return changeDataBuffConfig.getBuffTag();
    }

}
