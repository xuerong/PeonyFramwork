package com.peony.demo.assembly.battle.round;

import com.peony.demo.assembly.battle.Battle;
import com.peony.demo.assembly.battle.round.battleReport.BattleReport;
import com.peony.demo.assembly.battle.round.hero.BasicDataType;
import com.peony.demo.assembly.battle.round.skill.Skill;
import com.peony.demo.assembly.battle.round.skill.TargetType;
import com.peony.demo.assembly.battle.round.skill.TriggerTime;
import com.peony.demo.config.core.Tuple3;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 战斗对象
 *
 * @Author: zhengyuzhen
 * @Date: 2019-09-15 14:08
 */
@Slf4j
public class RoundBattle implements Battle {
    static final int ONE_MAX_CONSECUTIVE_FIGHT_COUNT = 100;
    static final int MAX_ROUND = 15;

    private int round;
    public String curHeroKey;

    private int winCamp;

    /**
     * 站位
     */
    public String[][] pos;

    public Map<String, BattleUnit> aliveBattleUnitMap = new LinkedHashMap<>();

    public Map<String, BattleUnit> deadBattleUnitMap = new LinkedHashMap<>();


    public RoundBattle(List<BattleUnit[][]> battleUnitList){


        // 通过某种方式，确定所有英雄的站位
        int allRow = 0;
        for(BattleUnit[][] battleUnits:battleUnitList){
            allRow += battleUnits.length;
        }
        String[][] pos = new String[allRow][];
        int row = 0;
        for(BattleUnit[][] battleUnits:battleUnitList){
            for(int i=0;i<battleUnits.length;i++){

                pos[row] = new String[battleUnits[i].length];

                for(int j=0;j<battleUnits[i].length;j++){
                    BattleUnit battleUnit = battleUnits[i][j];
                    battleUnit.setRoundBattle(this);
                    aliveBattleUnitMap.put(battleUnit.key(),battleUnit);
                    pos[row][j] = battleUnit.key();
                }

                row++;
            }
        }

        this.pos = pos;
    }

    @Override
    public void fight() {
        // 分配技能触发时间
        for(Map.Entry<String,BattleUnit> entry: aliveBattleUnitMap.entrySet()){
            for(Skill skill : entry.getValue().skillList){
                Tuple3<Integer,Integer,Integer> tuple3 =  skill.skillConfig.getTriggerTargetType();
                List<BattleUnit> battleUnitList = TargetType.valueOf(tuple3.getFirst()).getTarget(entry.getValue(),this,null,tuple3.getSecond(),tuple3.getThird());
                for(BattleUnit battleUnit:battleUnitList){
                    battleUnit.addSkillTriggerTime(TriggerTime.valueOf(skill.skillConfig.getTriggerTime()),skill);
                }
            }
        }
        // @BattleReport
        BattleReport.begin(aliveBattleUnitMap,pos);
        try{
            // 英雄初始化
            log.info("init---------------------- round = 0");
            for(Map.Entry<String,BattleUnit> entry: aliveBattleUnitMap.entrySet()){
                curHeroKey = entry.getValue().key();
                entry.getValue().init();
            }
            // 受限制类型初始化
            for(Map.Entry<String,BattleUnit> entry: aliveBattleUnitMap.entrySet()){
                curHeroKey = entry.getValue().key();
                Map<BasicDataType,Long> map = entry.getValue().basicData.initDefaultData();
                BattleReport.initDefaultData(entry.getValue().key());
            }
            // 初始化完成后的校验
            if(checkWin(false)){
                log.error("fight error,win while init!");
                return;
            }
            //
            log.info("fight----------------------");
            round = 1; // 从第一回合开始
            roundEnd:
            while (round<MAX_ROUND){
                log.info("第{}回合开始",round);
                // 回合开始
                for(Map.Entry<String,BattleUnit> entry: aliveBattleUnitMap.entrySet()){
                    curHeroKey = entry.getValue().key();
                    entry.getValue().roundBegin();
                }
                if(checkWin(true)){
                    break roundEnd;
                }
                // 战斗:按照速度处理，每回合速度都可能变化
                List<BattleUnit> battleUnitList = new ArrayList<>(aliveBattleUnitMap.values());

                while (battleUnitList.size()>0){
                    Collections.sort(battleUnitList);
                    BattleUnit battleUnit = battleUnitList.remove(0);
                    curHeroKey = battleUnit.key();
                    if(!battleUnit.alive()){
                        continue;
                    }
                    for (int i=0;i<ONE_MAX_CONSECUTIVE_FIGHT_COUNT;i++){
                        boolean fightAgain = battleUnit.fight();
                        if(checkWin(true)){
                            break roundEnd;
                        }
                        if(!fightAgain){
                            break;
                        }
                    }
                }
                // 回合结束
                for(Map.Entry<String,BattleUnit> entry: aliveBattleUnitMap.entrySet()){
                    curHeroKey = entry.getValue().key();
                    entry.getValue().roundEnd();
                }
                if(checkWin(true)){
                    break roundEnd;
                }
                // 判断输赢:如果只有一个阵营，就结束了
                log.info("第{}回合结束",round);
                //
                showInfo();
                //
                round++;
            }
            log.info("fight over");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            BattleReport battleReport = BattleReport.end(this.winCamp);
            log.info("\n\n\n"+battleReport.toJson());
        }

        // 显示所有属性
        showInfo();
    }

    private void showInfo(){
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        sb.append("活着的：").append(System.lineSeparator());
        for(Map.Entry<String, BattleUnit> entry:aliveBattleUnitMap.entrySet()){
            sb.append(entry.getValue().key()).append("\t").append(entry.getValue().basicData.toString()).append(System.lineSeparator());
        }
        sb.append("死掉的：").append(System.lineSeparator());
        for(Map.Entry<String, BattleUnit> entry:deadBattleUnitMap.entrySet()){
            sb.append(entry.getValue().key()).append("\t").append(entry.getValue().basicData.toString()).append(System.lineSeparator());
        }
        log.info(sb.toString());
    }

    void changeDeadMap(){
        Iterator<Map.Entry<String,BattleUnit>> iterator = aliveBattleUnitMap.entrySet().iterator();
        List<BattleUnit> deadBattleUnitList = new ArrayList<>();
        while (iterator.hasNext()){
            Map.Entry<String,BattleUnit> entry = iterator.next();
            if(!entry.getValue().alive()){
                iterator.remove();
                deadBattleUnitMap.put(entry.getKey(),entry.getValue());

                deadBattleUnitList.add(entry.getValue());
            }
        }
        if(deadBattleUnitList.size()>0){
            // 死亡事件
            for(BattleUnit battleUnit:deadBattleUnitList){
                battleUnit.onDead();
            }
        }
    }

    boolean checkWin(boolean changeDeadMap){
        if(changeDeadMap){
            changeDeadMap();
        }
        int camp = -1;
        boolean remainderOneCamp = true;
        for(Map.Entry<String,BattleUnit> entry: aliveBattleUnitMap.entrySet()){
            if(camp == -1){
                camp = entry.getValue().getCamp();
            }else if(camp != entry.getValue().getCamp()){
                remainderOneCamp = false;
                break;
            }
        }
        if(remainderOneCamp){
            Iterator<BattleUnit> battleUnitIterator = aliveBattleUnitMap.values().iterator();
            if(battleUnitIterator.hasNext()){
                this.winCamp = battleUnitIterator.next().camp;
                log.info("战斗结束,胜利的阵营是{}",this.winCamp);

            }else{
                log.warn("没有胜利者，都死了");
                this.winCamp = 0;
            }
        }
        return remainderOneCamp;
    }

    public int getRound() {
        return round;
    }
}
