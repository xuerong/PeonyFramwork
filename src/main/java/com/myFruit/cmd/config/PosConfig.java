package com.myFruit.cmd.config;

import java.util.HashMap;
import java.util.Map;

public class PosConfig {

    public static Map<Integer,PosConfig> datas = new HashMap<Integer,PosConfig>(){
        {
            put(0,new PosConfig(1,0));
            put(1,new PosConfig(1,0));
            put(2,new PosConfig(1,0));
            put(3,new PosConfig(1,0));
            put(4,new PosConfig(1,0));
            put(5,new PosConfig(2,20));
            put(6,new PosConfig(4,40));
            put(7,new PosConfig(6,80));
            put(8,new PosConfig(10,0,true));
            put(9,new PosConfig(15,160));
            put(10,new PosConfig(18,0,true));
            put(11,new PosConfig(22,320));
            put(12,new PosConfig(30,0,true));
            put(13,new PosConfig(38,640));
            put(14,new PosConfig(45,1280));
            put(15,new PosConfig(55,2560));
            put(16,new PosConfig(65,5120));
            put(17,new PosConfig(76,10240));
            put(18,new PosConfig(88,20480));
            put(19,new PosConfig(99,40960));


        }
    };

    /**
     * public static unlockPosCondition = {
     0:{level:1,gold:100},
     1:{level:1,gold:200},
     2:{level:1,gold:100},
     3:{level:1,gold:100},
     4:{level:1,gold:100},
     5:{level:1,gold:100},
     6:{level:2,gold:400},
     7:{level:4,gold:100},
     8:{level:8,gold:100},
     9:{level:10,gold:100},
     10:{level:11,gold:100},
     11:{level:12,gold:100},
     12:{level:13,gold:100},
     13:{level:14,gold:100},
     14:{level:15,gold:100},
     15:{level:16,gold:100},
     16:{level:17,gold:100},
     17:{level:18,gold:100},
     18:{level:19,gold:100},
     19:{level:20,gold:100},
     };
     */
    private int level;
    private int gold;
    private boolean share;

    PosConfig(int level,int gold){
        this.level = level;
        this.gold = gold;
        this.share = false;
    }
    PosConfig(int level,int gold,boolean share){
        this.level = level;
        this.gold = gold;
        this.share = share;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public boolean isShare() {
        return share;
    }

    public void setShare(boolean share) {
        this.share = share;
    }
}
