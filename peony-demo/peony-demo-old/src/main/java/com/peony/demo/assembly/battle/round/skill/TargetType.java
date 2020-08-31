package com.peony.demo.assembly.battle.round.skill;

import com.peony.demo.assembly.battle.round.BattleUnit;
import com.peony.demo.assembly.battle.round.RoundBattle;
import com.peony.demo.assembly.battle.round.hero.BasicDataType;
import com.peony.demo.assembly.battle.round.skill.Buff.Buff;
import com.peony.demo.assembly.battle.round.skill.Buff.BuffType;
import com.peony.common.tool.idenum.EnumUtils;
import com.peony.common.tool.idenum.IDEnum;
import com.peony.common.tool.util.Util;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-19 14:18
 */
public enum TargetType implements IDEnum,SkillTargetSelector {
    Null(0) {
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit, RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            return null;
        }
    },
    Inherit(1){
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit, RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            if(param1 == 0){
                return parentBattleUnitList;
            }
            List<BattleUnit> ret = new ArrayList<>();
            List<BattleUnit> filterList = BuffFilter.getTarget(oriBattleUnit, roundBattle, parentBattleUnitList, param1, param2);
            for(BattleUnit battleUnit:parentBattleUnitList){
                if(filterList.contains(battleUnit)){
                    ret.add(battleUnit);
                }
            }
            return ret;
        }
    },
    FirstEmpty(2) {
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            for(int i=0;i<roundBattle.pos.length;i++){
                for(int j=0;j<roundBattle.pos[i].length;j++){
                    String key = roundBattle.pos[i][j];
                    BattleUnit battleUnit = roundBattle.aliveBattleUnitMap.get(key);
                    if(battleUnit == null || !battleUnit.alive()){ // 死掉了
                        continue;
                    }
                    if(battleUnit.getCamp() != oriBattleUnit.getCamp()){
                        //
                        return Arrays.asList(battleUnit);
                    }
                }
            }
            throw new RuntimeException("error");
        }
    },
    AllEmpty(3){
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            List<BattleUnit> ret = new ArrayList<>();
            for(BattleUnit battleUnit:roundBattle.aliveBattleUnitMap.values()){
                if(battleUnit.getCamp() != oriBattleUnit.getCamp()){
                    ret.add(battleUnit);
                }
            }
            return ret;
        }
    },
    Random(4) {
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            // param1：随机位置：0（所有）1（第一排）
            // Param2: 随机个数
            switch (param1){
                case 0:
                    List<BattleUnit> list = new ArrayList<>();
                    for(BattleUnit battleUnit:roundBattle.aliveBattleUnitMap.values()){
                        if(battleUnit.getCamp() != oriBattleUnit.getCamp()){
                            list.add(battleUnit);
                        }
                    }
                    return Util.getRandomFrom(list,param2);
            }
            return null;
        }
    },
    BasicData(5) { // 根据某一属性
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            return null;
        }
    },
    Self(6) { // 根据某一属性
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            return Arrays.asList(oriBattleUnit);
        }
    },
    MinBloodFriendly(7) { // 根据某一属性
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            BattleUnit ret = null;
            for(BattleUnit battleUnit:roundBattle.aliveBattleUnitMap.values()){
                if(battleUnit.getCamp() == oriBattleUnit.getCamp()){
                    if(ret == null || ret.basicData.get(BasicDataType.Blood) > battleUnit.basicData.get(BasicDataType.Blood)){
                        ret = battleUnit;
                    }
                }
            }
            if(ret == null){
                return new ArrayList<>();
            }
            return Arrays.asList(ret);
        }
    },
    All(8){
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {
            List<BattleUnit> ret = new ArrayList<>();
            for(BattleUnit battleUnit:roundBattle.aliveBattleUnitMap.values()){
                ret.add(battleUnit);
            }
            return ret;
        }
    },
    BuffFilter(9){
        @Override
        public List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2) {

            BuffType buffType = BuffType.valueOf(param1);

            List<BattleUnit> ret = new ArrayList<>();
            for(BattleUnit battleUnit:roundBattle.aliveBattleUnitMap.values()){
                List<Buff> buffList = battleUnit.buffTypeListMap.get(buffType);
                if(CollectionUtils.isNotEmpty(buffList)){
                    for(Buff buff:buffList){
                        List<Integer> tags =  buff.getTags();
                        if(CollectionUtils.isNotEmpty(tags) && tags.contains(param2)){
                            ret.add(battleUnit);
                        }
                    }
                }
            }
            return ret;
        }
    },

    ;
    final int id;
    TargetType(int id){
        this.id = id;
    }
    public int getId(){
        return id;
    }

    public static TargetType valueOf(int id){
        return EnumUtils.getEnum(TargetType.class,id);
    }

}
interface SkillTargetSelector{
    List<BattleUnit> getTarget(BattleUnit oriBattleUnit,RoundBattle roundBattle,List<BattleUnit> parentBattleUnitList,int param1,int param2);
}
