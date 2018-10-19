package com.myFruit.cmd;

public class Cmd {
    public static final int UserInit = 10001;

    //

    public static final int GetUserBase = 20001;

    public static final int BagInfo = 20011;

    //
    public static final int TechnologyInfo = 20021;
    public static final int TechnologyUpLevel = 20022;

    //
    public static final int OrderInfo = 20031;
    public static final int FinishOrder = 20032;


    // 果树 20041
    public static final int FruitInfo = 20041;
    public static final int UnlockFruit = 20042;
    public static final int PlantFruit = 20043;
    public static final int HavestFruit = 20044;
    public static final int FertilizerFruit = 20045;

    // 肥料和加速 20051
    public static final int SkillInfo = 20051;
    public static final int SpeedUp = 20052;
    public static final int AddFertilizer = 20053; // 看广告增加肥料
    public static final int AddSpeedPower = 20054; // 看广告增加加速道具

    // 玩家好友 20061
    public static final int GetFriendList = 20061;  // 获取好友列表
    public static final int AddFriend = 20062; // 通过分享进来，调用这个加入好友
    public static final int GetFriendInfo = 20063; // 获取好友果树的信息
    public static final int VisitFriend = 20064; // 执行拜访


    //
    // 推送 20071
    public static final int Push_UnlockByShare = 20071; // 邀请到新玩家解锁果实的推送
    public static final int Push_NewFriendByShare = 20072; // 邀请到新玩家的推送

    // rank
    public static final int GetGlobalRank = 20081; // 全服排行

    // task
    public static final int GetTaskInfo = 20091; // 任务信息
    public static final int AwarkTask = 20092; // 领取任务
    public static final int PushTask = 20093; // 推送任务
}
