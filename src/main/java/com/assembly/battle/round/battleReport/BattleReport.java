package com.assembly.battle.round.battleReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.assembly.battle.round.BattleUnit;
import com.assembly.battle.round.hero.BasicData;
import com.assembly.battle.round.hero.BasicDataType;
import com.assembly.battle.round.hero.HeroUnit;
import com.assembly.battle.round.skill.Buff.Buff;
import com.assembly.battle.round.skill.Buff.BuffType;
import lombok.Data;
import netscape.javascript.JSObject;
import org.apache.commons.collections.MapUtils;

import java.util.*;

/**
 * 战报：
 * 1、英雄初始状态：基本属性，技能，buff
 * 2、每一回合行为，状态变化
 * 3、每一回合又分为每一个小回合（每个人），然后对应的行为。
 *
 * @Author: zhengyuzhen
 * @Date: 2019-10-03 07:58
 */
public class BattleReport {

    private static ThreadLocal<BattleReport> battleReportThreadLocal = new ThreadLocal<>();

    private BattleReport(Map<String, BattleUnit> battleUnitMap,String[][] pos){
        this.pos = pos;
        Map<String,Hero> heroMap = new HashMap<>();
        for(Map.Entry<String,BattleUnit> entry:battleUnitMap.entrySet()){
            Hero hero = new Hero();

            BattleUnit battleUnit = entry.getValue();
            hero.key = battleUnit.key();
            Map<BasicDataType,Long> basicDataLongMap = new LinkedHashMap<>();
            for(Map.Entry<BasicDataType,Long> basicDataTypeLongEntry:battleUnit.basicData.getValues().entrySet()){
                basicDataLongMap.put(basicDataTypeLongEntry.getKey(),basicDataTypeLongEntry.getValue());
            }
            hero.basicData = new BasicData(basicDataLongMap);
            if (MapUtils.isNotEmpty(battleUnit.buffTypeListMap)) {
                hero.buffListMap = new HashMap<>();
                for(Map.Entry<BuffType, List<Buff>> entry1:battleUnit.buffTypeListMap.entrySet()){
                    List<Integer> buffId = new ArrayList<>();
                    for(Buff buff:entry1.getValue()){
                        buffId.add(buff.getId());
                    }
                    hero.buffListMap.put(entry1.getKey(),buffId);
                }
            }

            HeroUnit heroUnit = (HeroUnit)battleUnit;
            hero.heroConfigId = heroUnit.heroConfig.getId();
            heroMap.put(entry.getKey(),hero);
        }
        this.battleUnitMap = heroMap;
    }

    public static void begin(Map<String, BattleUnit> battleUnitMap,String[][] pos){
        battleReportThreadLocal.set(new BattleReport(battleUnitMap,pos));
    }
    public static BattleReport end(int winner){
        BattleReport ret = battleReportThreadLocal.get();
        ret.winner = winner;
        battleReportThreadLocal.remove();
        return ret;
    }

    public static void initDefaultData(String heroKey){
        BattleReport battleReport = battleReportThreadLocal.get();
        Hero hero = battleReport.battleUnitMap.get(heroKey);
        hero.basicData.initDefaultData();
    }

    public static void putSkillOper(int round,String curHeroKey/*本回合本次行动的位置,对应本次触发行动的英雄的位置*/,
                                    SkillOperData skillOperData){
        OnceRound onceRound = getOnceRound(round);
        if(onceRound.heroOperMap == null){
            onceRound.heroOperMap = new LinkedHashMap<>(); // 按照放入的顺序排序
        }
        List<SkillOperData> skillOperList = onceRound.heroOperMap.get(curHeroKey);
        if(skillOperList == null){
            skillOperList = new ArrayList<>();
            onceRound.heroOperMap.put(curHeroKey,skillOperList);
        }
        skillOperList.add(skillOperData);
        //
    }

    private static OnceRound getOnceRound(int round){
        //
        BattleReport ret = battleReportThreadLocal.get();
        if(ret.roundMap == null){
            ret.roundMap = new LinkedHashMap<>();
        }
        //
        OnceRound onceRound = ret.roundMap.get(round);
        if(onceRound == null){
            onceRound = new OnceRound();
            ret.roundMap.put(round,onceRound);
        }
        return onceRound;
    }



    private String[][] pos;
    private Map<String,Hero> battleUnitMap;
    private Map<Integer,OnceRound> roundMap;
    private int winner; // 胜利者的阵营

    private static class Hero{
        private String key;
        private int heroConfigId;
        private BasicData basicData;
        private Map<BuffType, List<Integer>> buffListMap;

        public JSONObject toJson(){
            JSONObject ret = new JSONObject();
            ret.put("key",key);
            ret.put("heroConfigId",heroConfigId);
            ret.put("basicData",basicData.toJson());
            if(MapUtils.isNotEmpty(buffListMap)){
                JSONObject buffs = new JSONObject();
                for(Map.Entry<BuffType, List<Integer>> entry:buffListMap.entrySet()){
                    JSONArray array = new JSONArray();
                    for(Integer buffId:entry.getValue()){
                        array.add(buffId);
                    }
                    buffs.put(String.valueOf(entry.getKey().getId()),array);
                }
                ret.put("buffs",buffs);
            }
            return ret;
        }

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append(key+"["+heroConfigId+"]");
            for(Map.Entry<BasicDataType,Long> entry:basicData.getValues().entrySet()){
                sb.append("[").append(entry.getKey()).append(":").append(entry.getValue()).append("]");
            }
            if(MapUtils.isNotEmpty(buffListMap)){
                for(Map.Entry<BuffType, List<Integer>> entry:buffListMap.entrySet()){
                    sb.append(entry.getKey()).append("[");
                    for(Integer buffId:entry.getValue()){
                        sb.append(buffId).append(",");
                    }
                    sb.append("]");
                }
            }
            return sb.toString();
        }

        public String execRelease(SkillOperData skillOperData){
            StringBuilder sb = new StringBuilder();
            switch (skillOperData.operType){
                case DelBuffRelease:
                case SkillRelease:
                case BuffRelease:
                    long lastValue = basicData.changeBasicData(skillOperData.basicDataType,skillOperData.changeValue);
                    sb.append("，剩余"+lastValue);
                    break;
                case AddBuff:
                    if(buffListMap == null){
                        buffListMap = new HashMap<>();
                    }
                    List<Integer> buffIdList = buffListMap.get(skillOperData.buffType);
                    if(buffIdList == null){
                        buffIdList = new ArrayList<>();
                        buffListMap.put(skillOperData.buffType,buffIdList);
                    }
                    buffIdList.add(skillOperData.buffId);
                    break;
                case DelBuff:
                    buffIdList = buffListMap.get(skillOperData.buffType);
                    Iterator<Integer> it = buffIdList.iterator();
                    boolean remove = false;
                    while (it.hasNext()){
                        if(it.next() == skillOperData.buffId){
                            it.remove();
                            remove = true;
                            break;
                        }
                    }
                    if(!remove){
                        throw new RuntimeException("error! buff is not exist while remove!buffType="+skillOperData.buffType+",buffId="+skillOperData.buffType);
                    }
                    break;
            }
            return sb.toString();
        }

    }

    private static class OnceRound{
        private Map<String,List<SkillOperData>> heroOperMap; // 12个
    }
    @Data
    public static class SkillOperData{
        OperType operType;
        //
        String triggerHeroKey; // 触发英雄
        String heroKey; // 变化英雄
        //
        int skillConfigIdId; // 技能configId
        // 基础值改变
        BasicDataType basicDataType;
        long changeValue;
        // buff添加，移除
        BuffType buffType;
        Integer buffId;



        // 技能释放用的构造函数
        public SkillOperData(String triggerHeroKey,String heroKey,int skillConfigIdId,BasicDataType basicDataType,long changeValue){
            operType = OperType.SkillRelease;
            this.triggerHeroKey = triggerHeroKey;
            this.heroKey = heroKey;
            this.skillConfigIdId = skillConfigIdId;
            this.basicDataType = basicDataType;
            this.changeValue = changeValue;
        }
        // buff添加或删除的构造函数
        public SkillOperData(String triggerHeroKey,String heroKey,boolean buffAdd,BuffType buffType,Integer buffId){
            operType = buffAdd?OperType.AddBuff:OperType.DelBuff;
            this.triggerHeroKey = triggerHeroKey;
            this.heroKey = heroKey;
            this.buffType = buffType;
            this.buffId = buffId;
        }
        // buff释放时的构造函数
        public SkillOperData(String triggerHeroKey,String heroKey,BuffType buffType,Integer buffId,BasicDataType basicDataType,long changeValue,OperType operType){
            this.operType = operType;
            this.triggerHeroKey = triggerHeroKey;
            this.heroKey = heroKey;
            this.buffType = buffType;
            this.buffId = buffId;
            this.basicDataType = basicDataType;
            this.changeValue = changeValue;
        }

        public JSONObject toJson(){
            JSONObject ret = new JSONObject();
            ret.put("operType",operType.id);
            ret.put("triggerHeroKey",triggerHeroKey);
            ret.put("heroKey",heroKey);
            ret.put("skillConfigIdId",skillConfigIdId);
            ret.put("basicDataType",basicDataType==null?0:basicDataType.getId());
            ret.put("changeValue",changeValue);
            ret.put("buffType",buffType == null?0:buffType.getId());
            ret.put("buffId",buffId);
            return ret;
        }

        @Override
        public String toString(){
            //
            StringBuilder sb = new StringBuilder(operType.name+"\t");
            switch (operType){
                case SkillRelease:
                    sb.append("[").append(triggerHeroKey).append("]向[").append(heroKey).append("]释放skill，skillConfigIdId=").append(skillConfigIdId)
                            .append("，basicDataType=").append(basicDataType).append(",changeValue=").append(changeValue);
                    break;
                case AddBuff:
                    sb.append("[").append(triggerHeroKey).append("]向[").append(heroKey).append("]添加buff，buffType=").append(buffType).append("，buffId=").append(buffId);
                    break;
                case BuffRelease:
                    sb.append("[").append(triggerHeroKey).append("]添加给[").append(heroKey).append("]的buff释放效果，buffType=").append(buffType).append("，buffId=").append(buffId)
                            .append("，basicDataType=").append(basicDataType).append(",changeValue=").append(changeValue);
                    break;
                case DelBuff:
                    sb.append("[").append(triggerHeroKey).append("]添加给[").append(heroKey).append("]的buff删除，buffType=").append(buffType).append("，buffId=").append(buffId);
                    break;
                case DelBuffRelease:
                    sb.append("[").append(triggerHeroKey).append("]添加给[").append(heroKey).append("]的buff删除时恢复，buffType=").append(buffType).append("，buffId=").append(buffId)
                            .append("，basicDataType=").append(basicDataType).append(",changeValue=").append(changeValue);
                    break;
            }
            return sb.toString();
        }
    }

    public enum OperType{
        SkillRelease(1,"释放技能"),
        AddBuff(2,"添加buff"),
        BuffRelease(3,"释放buff"),
        DelBuff(4,"删除buff"),
        DelBuffRelease(5,"删除buff回收效果"),
        ;
        final int id;
        final String name;
        OperType(int id,String name){
            this.id = id;
            this.name = name;
        }

    }




    public JSONObject toJson(){
        /**
         * private String[][] pos;
         *     private Map<String,Hero> battleUnitMap;
         *     private Map<Integer,OnceRound> roundMap;
         *     private boolean win; // 进攻方是否赢了
         */
        JSONObject ret = new JSONObject();
        // pos
        JSONArray posArray = new JSONArray();
        for(String[] poses:pos){
            JSONArray array = new JSONArray();
            for(String heroId:poses){
                array.add(heroId);
            }
            posArray.add(array);
        }
        ret.put("pos",posArray);
        // battleUnitMap
        JSONObject battleUnit = new JSONObject();
        for(Map.Entry<String,Hero> entry:battleUnitMap.entrySet()){
            battleUnit.put(entry.getKey(),entry.getValue().toJson());
        }
        ret.put("battleUnit",battleUnit);
        // heroOpers
        JSONObject heroOpers = new JSONObject();
        for(Map.Entry<Integer,OnceRound> onceRoundEntry:roundMap.entrySet()){
            JSONObject heroOper = new JSONObject();
            for(Map.Entry<String,List<SkillOperData>> entry:onceRoundEntry.getValue().heroOperMap.entrySet()){
                JSONArray array = new JSONArray();
                for(SkillOperData skillOperData:entry.getValue()){
                    array.add(skillOperData.toJson());
                }
                heroOper.put(entry.getKey(),array);
            }
            heroOpers.put(String.valueOf(onceRoundEntry.getKey()),heroOper);
        }
        ret.put("heroOpers",heroOpers);
        //
        ret.put("winner",winner);
        return ret;
    }


    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        // 英雄初始状态
        sb.append("英雄初始状态:\n");
        for(Hero hero:battleUnitMap.values()){
            sb.append(hero.toString()).append("\n");
        }
        sb.append("\n\n\n");
        // 战斗过程
        for(Map.Entry<Integer,OnceRound> onceRoundEntry:roundMap.entrySet()){
            int rount = onceRoundEntry.getKey();
            OnceRound onceRound = onceRoundEntry.getValue();
            if(rount == 0){
                sb.append("------------------------------init\n");
            }else{
                sb.append("-------------------------------rount ").append(rount).append("\n");
            }
            for(Map.Entry<String,List<SkillOperData>> entry:onceRound.heroOperMap.entrySet()){
                //
                sb.append("\t").append(entry.getKey()).append(":\n");
                for(SkillOperData skillOperData:entry.getValue()){
                    // 执行
                    sb.append("\t\t").append(skillOperData.toString());
                    Hero hero = battleUnitMap.get(skillOperData.heroKey);
                    String last = hero.execRelease(skillOperData);
                    sb.append(last).append("\n");
                }
            }
            //
        }
        // 英雄结束状态
        sb.append("英雄结束状态:\n");
        for(Hero hero:battleUnitMap.values()){
            sb.append(hero.toString()).append("\n");
        }
        sb.append("\n\n\n");
        return sb.toString();
    }
}
