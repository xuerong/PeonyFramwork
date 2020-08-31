package com.peony.demo.myFruit.cmd.config;

public class ExpConfig {
    public static int getUpLevelExp(int level){
        return (int)(Math.sqrt((long)(level-1)*(level-1)*(level-1))*30)+20;
    }
}
