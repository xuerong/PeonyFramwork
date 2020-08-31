package com.peony.demo.assembly.battle.round.skill.Buff.buff;

import com.peony.demo.assembly.battle.round.BattleUnit;
import com.peony.demo.assembly.battle.round.RoundBattle;
import com.peony.demo.assembly.battle.round.skill.Buff.Buff;
import com.peony.demo.assembly.config.HardControlBuffConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-19 20:23
 */
@Slf4j
public class HardControlBuff extends Buff {
    public HardControlBuffConfig hardControlBuffConfig;

    public HardControlBuff(){
    }
    @Override
    public void releaseSkill(BattleUnit oriBattleUnit, RoundBattle roundBattle) {
        // 啥都不做
    }

    @Override
    public void doRemove(BattleUnit oriBattleUnit, RoundBattle roundBattle) {
        log.info("{} 硬控buff解除",oriBattleUnit.key());
    }

    @Override
    public int getId() {
        return hardControlBuffConfig.getId();
    }

    @Override
    public List<Integer> getTags() {
        return hardControlBuffConfig.getBuffTag();
    }

}
