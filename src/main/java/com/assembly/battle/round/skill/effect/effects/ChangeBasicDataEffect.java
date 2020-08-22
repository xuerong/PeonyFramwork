package com.assembly.battle.round.skill.effect.effects;

import com.assembly.battle.BattleUtil;
import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.RoundBattle;
import com.assembly.battle.round.battleReport.BattleReport;
import com.assembly.battle.round.hero.BasicDataType;
import com.assembly.battle.round.skill.Skill;
import com.assembly.battle.round.skill.TargetType;
import com.assembly.battle.round.skill.effect.Effect;
import com.assembly.battle.round.skill.effect.EffectType;
import com.assembly.config.ChangeDataChangeEffectConfig;
import com.assembly.config.ChangeDataEffectConfig;
import com.google.common.collect.ImmutableList;
import com.peony.engine.config.core.Tuple3;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-24 10:03
 */
@Slf4j
public class ChangeBasicDataEffect extends Effect {
    private final ChangeDataEffectConfig changeDataEffectConfig;
    public List<ChangeDataChangeEffectConfig> changeDataChangeEffectConfigList = new ArrayList<>();


    public ChangeBasicDataEffect(Skill skill,ChangeDataEffectConfig changeDataEffectConfig){
        super(skill);
        this.changeDataEffectConfig = changeDataEffectConfig;
        if(MapUtils.isNotEmpty(changeDataEffectConfig.getAttachEffectIdListMap())){
            attachEffectList = new ArrayList<>();
            for(Map.Entry<Integer, ImmutableList<Integer>> entry: changeDataEffectConfig.getAttachEffectIdListMap().entrySet()){
                for(Integer effectId:entry.getValue()){
                    attachEffectList.add(EffectType.valueOf(entry.getKey()).create(skill,effectId));
                }
            }
        }
    }

    @Override
    public List<BattleUnit> releaseSkill(BattleUnit oriBattleUnit, RoundBattle roundBattle, List<BattleUnit> skillTarget) {
        List<BattleUnit> battleUnitList = getFinalTarget(oriBattleUnit,roundBattle,skillTarget);

        List<BattleUnit> ret = new ArrayList<>();

        for(BattleUnit battleUnit:battleUnitList){
            boolean isDodge = false;
            if(changeDataEffectConfig.isCanDodged()){
                // 可以被闪避的话，要根据对方的闪避，和自己的命中确定是否闪避
                long dodge = battleUnit.basicData.get(BasicDataType.Dodge);
                if(dodge>0){
                    isDodge = BattleUtil.randomRatio(dodge);
                }
            }
            if(!isDodge){
                long value = BattleUtil.calChangeBasicData(oriBattleUnit,battleUnit, changeDataEffectConfig.getValueTypeId(),getFinalValue()); //changeDataEffectConfig.valueTypeId.cal(oriBattleUnit,roundBattle,changeDataEffectConfig.value);

                if(changeDataEffectConfig.getBasicDataType() == BasicDataType.Blood.getId()){
                    value = BattleUtil.calAttackValue(value,oriBattleUnit,battleUnit, changeDataEffectConfig.isCanCrit(), changeDataEffectConfig.isCanDefend());
                }

                BasicDataType basicDataType = BasicDataType.valueOf(changeDataEffectConfig.getBasicDataType());
                battleUnit.basicData.changeBasicData(basicDataType,value);
                log.info("{}:\t{}[{}]->{}[{}]\t{}\t剩余{}", changeDataEffectConfig.getName(),oriBattleUnit.getCamp(),oriBattleUnit.key(),battleUnit.getCamp(),
                        battleUnit.key(),value,battleUnit.basicData.get(basicDataType));
                ret.add(battleUnit);
                // @BattleReport
                BattleReport.putSkillOper(roundBattle.getRound(),roundBattle.curHeroKey,
                        new BattleReport.SkillOperData(oriBattleUnit.key(),battleUnit.key(),skill.skillConfig.getId(),basicDataType,value));
            }else{
                log.info("{}:\t{}[{}]->{}[{}]\t闪避", changeDataEffectConfig.getName(),oriBattleUnit.getCamp(),oriBattleUnit.key(),battleUnit.getCamp(),battleUnit.key());
            }
        }
        return ret;
    }

    public List<BattleUnit> getFinalTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> skillTarget){
        //
        Tuple3<Integer,Integer,Integer> tuple3 =  changeDataEffectConfig.getTargetType();

        for(ChangeDataChangeEffectConfig config: changeDataChangeEffectConfigList){
            // 改变目标
            Tuple3<Integer,Integer,Integer> newTargetTuple3 =  config.getNewTargetType();

            if(newTargetTuple3.getFirst() != TargetType.Null.getId()){
                tuple3 = newTargetTuple3;
            }
        }

        List<BattleUnit> battleUnitList = TargetType.valueOf(tuple3.getFirst()).getTarget(oriBattleUnit,roundBattle,skillTarget,tuple3.getSecond(),tuple3.getThird());

        return battleUnitList;
    }

    public long getFinalValue(){
        //
        long changeValue = 0;
        long replaceValue = changeDataEffectConfig.getValue(); // 先replace，再change
        for(ChangeDataChangeEffectConfig config: changeDataChangeEffectConfigList){
            // 改变值
            switch (config.getChangeType()){
                case 1:
                    changeValue+=config.getChangeValue();
                    break;
                case 2:
                    replaceValue = config.getChangeValue();
                    break;
            }
        }

        return replaceValue+changeValue;
    }

    @Override
    public int getId() {
        return changeDataEffectConfig.getId();
    }

}
