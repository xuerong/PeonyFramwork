package com.myFruit.cmd.config;

import java.util.*;

/**
 * public static fruits = {
 0:{id:0,res:"zuowu_caomei",name:"草莓",gold:5,time:6,shu:ShuXiang.Huo,unlockLevel:1},
 1:{id:1,res:"zuowu_ganlan",name:"橄榄",gold:10,time:12,shu:-1,unlockLevel:2},
 2:{id:2,res:"zuowu_juzi",name:"橘子",gold:20,time:24,shu:-1,unlockLevel:4},
 3:{id:3,res:"zuowu_lanmei",name:"蓝莓",gold:40,time:48,shu:ShuXiang.Shui,unlockLevel:8},
 4:{id:4,res:"zuowu_nangua",name:"南瓜",gold:80,time:96,shu:ShuXiang.Tu,unlockLevel:16},
 5:{id:5,res:"zuowu_ningmeng",name:"柠檬",gold:160,time:2000,shu:ShuXiang.Jin,unlockLevel:28},
 6:{id:6,res:"zuowu_pingguo",name:"苹果",gold:320,time:4000,shu:-1,unlockLevel:48},
 7:{id:7,res:"zuowu_xihongshi",name:"西红柿",gold:640,time:8000,shu:-1,unlockLevel:68},
 8:{id:8,res:"zuowu_yangcong",name:"洋葱",gold:1280,time:16000,shu:-1,unlockLevel:88},
 9:{id:9,res:"zuowu_yumi",name:"玉米",gold:2560,time:32000,shu:ShuXiang.Mu,unlockLevel:128}
 };
 */

public class FruitConfig {

    static Map<Integer,List<FruitConfig>> unlockedByLevel = new HashMap<>();


    public static Map<Integer,FruitConfig> datas = new TreeMap<Integer,FruitConfig>(){
        {
            put(0,new FruitConfig(0,5,      7,      60,      4,  1,1));
            put(1,new FruitConfig(1,8,     11,     120,      -1, 1,2));
            put(2,new FruitConfig(2,10,     14,     240,      -1, 1,3));
            put(3,new FruitConfig(3,12,     17,     480,      3,  2,4));
            put(4,new FruitConfig(4,15,     21,     720,      5,  4,6));
            put(5,new FruitConfig(5,18,     35,     1080,      1,  6,8));
            put(6,new FruitConfig(6,20,     48,     1800,      -1, 9,10));
            put(7,new FruitConfig(7,24,    63,    3600,      -1, 12,12));
            put(8,new FruitConfig(8,25,    85,    5400,      -1, 15,16));
            put(9,new FruitConfig(9,30,    120,    7200,      2,  18,20));
        }
    };

    public static int randomFruit(){
        return (int)(Math.random()*datas.size());
    }

    public static List<FruitConfig> getUnlockedFruit(int level){
        List<FruitConfig> ret = unlockedByLevel.get(level);
        if(ret !=null){
            return ret;
        }
        ret = new ArrayList<>();
        for(Map.Entry<Integer,FruitConfig> entry:datas.entrySet()){
            if(entry.getValue().getUnlockLevel() <= level){
                ret.add(entry.getValue());
            }
        }
        unlockedByLevel.put(level,ret);
        return ret;
    }


    public FruitConfig(int id,int gold,int sellGold,int time,int shu,int unlockLevel,int exp){
        this.id = id;
        this.gold = gold;
        this.sellGold = sellGold;
        this.time = time;
        this.shu = shu;
        this.unlockLevel = unlockLevel;
        this.exp = exp;
    }

    private int id;
    private int gold;
    private int time;
    private int shu;
    private int unlockLevel;
    private int sellGold; // 卖的价格，订单里的价格以这个为基准.....单位时间的溢价要越来越高
    private int exp; // 收获时的经验

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getShu() {
        return shu;
    }

    public void setShu(int shu) {
        this.shu = shu;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public void setUnlockLevel(int unlockLevel) {
        this.unlockLevel = unlockLevel;
    }

    public int getSellGold() {
        return sellGold;
    }

    public void setSellGold(int sellGold) {
        this.sellGold = sellGold;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }
}
