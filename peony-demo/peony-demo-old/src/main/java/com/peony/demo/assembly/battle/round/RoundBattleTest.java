package com.peony.demo.assembly.battle.round;

import com.peony.demo.assembly.battle.BattleUtil;
import com.peony.demo.assembly.battle.round.hero.HeroUnit;
import com.peony.demo.assembly.config.HeroConfig;
import com.peony.demo.assembly.config.HeroContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-19 08:47
 */
public class RoundBattleTest {
    public static void main(String[] args){


        HeroConfig heroConfig1 = BattleUtil.configService.getItem(HeroContainer.class,1);

        HeroConfig heroConfig2 = BattleUtil.configService.getItem(HeroContainer.class,2);

//        HeroConfig heroConfig = HeroConfig.get(10);

        for(int t=0;t<1;t++){
            long begin = System.currentTimeMillis();

            List<BattleUnit[][]> battleUnitList = new ArrayList<>();
            for(int camp = 0;camp<2;camp++){
                HeroConfig heroConfig = camp == 0?heroConfig1:heroConfig2;

                BattleUnit[][] battleUnits = new BattleUnit[2][];
                for(int i=0;i<battleUnits.length;i++){
                    battleUnits[i] = new BattleUnit[3];
                    for(int j=0;j<battleUnits[i].length;j++){
                        BattleUnit battleUnit = new HeroUnit(camp,heroConfig.getName()+"_"+camp+"_"+i+"_"+j,heroConfig);
                        battleUnits[i][j] = battleUnit;
                    }
                }
                battleUnitList.add(battleUnits);
            }

            RoundBattle roundBattle = new RoundBattle(battleUnitList);

            //

            roundBattle.fight();
            long end = System.currentTimeMillis();

            System.out.println(end-begin);
        }




    }
}
