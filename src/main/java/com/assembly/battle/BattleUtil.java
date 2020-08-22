package com.assembly.battle;

import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.hero.BasicDataType;
import com.assembly.battle.round.skill.Skill;
import com.assembly.battle.round.skill.TargetType;
import com.assembly.battle.round.skill.effect.Effect;
import com.assembly.battle.round.skill.effect.EffectType;
import com.assembly.config.ChangeDataTypeConfig;
import com.assembly.config.ChangeDataTypeContainer;
import com.peony.engine.config.ConfigService;
import com.peony.engine.config.core.Tuple3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-24 18:28
 */
public class BattleUtil {

    public final static ConfigService configService;

    static {
        configService = new ConfigService();
        configService.init();
    }

    static Random random = new Random();
    public static boolean randomRatio(long value){
        return random.nextInt(10000)<value;
    }
    public static long calByAddition(long value,long addition){
        return value+value*addition/10000;
    }

    public static long calChangeBasicData(BattleUnit from,BattleUnit to,int valueTypeId,long value){
        ChangeDataTypeConfig config = BattleUtil.configService.getItem(ChangeDataTypeContainer.class,valueTypeId);
        long ret;
        if(config.isAbsolute()){
            ret = value;
        }else{
            if(config.isFrom()){
                ret = from.basicData.get(BasicDataType.valueOf(config.getBasicDataType())) * value/10000;
            }else{
                ret = to.basicData.get(BasicDataType.valueOf(config.getBasicDataType())) * value/10000;
            }
        }
        return ret;
    }

    public static List<Effect> getEffectListByNavigation(List<BattleUnit> battleUnitList,int targetSkillId,EffectType targetEffectType
            ,int targetEffectId,GetEffectListByNavigationCallBack call){
        List<Effect> ret = new ArrayList<>();
        for(BattleUnit battleUnit:battleUnitList){
            for(Skill skill:battleUnit.skillList){
                if(skill.skillConfig.getId() == targetSkillId){
                    for(Effect effect:skill.effects){
                        if(effect.getId() == targetEffectId){
                            EffectType effectType = effect.getEffectType();
                            if(effectType == targetEffectType){
                                ret.add(effect);
                                if(call != null){
                                    call.call(battleUnit,effect);
                                }
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    public static interface GetEffectListByNavigationCallBack{
        void call(BattleUnit battleUnit,Effect effect);
    }

    public static long calAttackValue(long value,BattleUnit oriBattleUnit,BattleUnit battleUnit,boolean canCrit,boolean canDefend){
        if(canCrit){
            long cirtRate = oriBattleUnit.basicData.get(BasicDataType.CritRate);
            if(cirtRate > 0){
                if(BattleUtil.randomRatio(cirtRate)){
                    // 发生暴击
                    long defaultCritAttackRate = 16000;
                    long critAttackRate = defaultCritAttackRate + oriBattleUnit.basicData.get(BasicDataType.CritAttackAddition);
                    value = value * critAttackRate/10000;
                }
            }
        }
        if(canDefend){
            // 护甲扣除
            /**
             * 当护甲大于等于零时：
             * 实际对你造成的物理伤害=100/(100+护甲值)；
             * 实际生命值=最大生命值*(100+护甲值)/100；
             *
             * 当护甲少于零时：
             * 实际对你造成的物理伤害=1-护甲值/100；
             * 实际生命值=最大生命值*100/(100-护甲值)。
             */
            long defend =  battleUnit.basicData.get(BasicDataType.Defend);
            // 破防
            defend -= (oriBattleUnit.basicData.get(BasicDataType.CrackDefend));
            //
            if(defend > 0){
                value = value*100/(100 + defend);
            }else if(defend < 0){
                value = value*(100 - defend) / 100;
            }
        }
        // 真实伤害加成
        long realHarmAddition = oriBattleUnit.basicData.get(BasicDataType.RealHarmAddition);
        if(realHarmAddition > 0){
            value = BattleUtil.calByAddition(value,realHarmAddition);
        }
        long reductionRate = oriBattleUnit.basicData.get(BasicDataType.ReductionRate);
        if(reductionRate > 0){
            if(reductionRate>7000){
                reductionRate = 7000;
            }
            value = BattleUtil.calByAddition(value,reductionRate);
        }
        return value;
    }
}
