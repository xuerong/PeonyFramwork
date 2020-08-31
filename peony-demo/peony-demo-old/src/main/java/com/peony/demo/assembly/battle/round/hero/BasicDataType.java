package com.peony.demo.assembly.battle.round.hero;

import com.peony.common.tool.idenum.EnumUtils;
import com.peony.common.tool.idenum.IDEnum;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-21 15:00
 */
public enum BasicDataType implements IDEnum {

    // 一些限制大值
    FullBlood(15,false,null,0),
    FullMagic(16,false,null,0),

    Attack(1,false,null,0),
    Defend(2,true,null,0),
    Blood(3,false,BasicDataType.FullBlood,100),
    Speed(4,false,null,0),

    Magic(5,false,BasicDataType.FullMagic,50),
    Dodge(6,false,null,0), // 闪避，万分比

    //
    CrackDefend(7,false,null,0), // 破防，本次攻击，减对方防御力
    SkillAttack(8,true,null,0), // 技能伤害加成 TODO 这个暂时没用，因为没有定义技能伤害这个概念，可以加在Effect里面
    HitRate(9,true,null,0), // 命中，减闪避
    CritRate(10,false,null,0), // 暴击率，作用在技能的直接ChangeBasicDataEffect上
    CritAttackAddition(11,false,null,0), // 暴击伤害加成，默认是1.6倍
    RealHarmAddition(12,false,null,0), // 真实伤害加成
    ReductionRate(13,false,null,0), // 减伤率
    ExemptionControlRate(14,false,null,0), // 免控率，限制控制类技能


    ;

    final int id;
    final boolean mayBeNegative;
    final BasicDataType fullLimitType; // 限制类型，比如血量（Blood）受限于最大血量（FullBlood）
    final int defaultInitRate; // 受限制的类型，默认初始百分比，只有在受限制的类型上生效

    BasicDataType(int id,boolean mayBeNegative,BasicDataType fullLimitType,int defaultInitRate){
        this.id = id;
        this.mayBeNegative = mayBeNegative;
        this.fullLimitType = fullLimitType;
        this.defaultInitRate = defaultInitRate;
    }

    @Override
    public int getId() {
        return id;
    }


    public boolean isMayBeNegative() {
        return mayBeNegative;
    }

    public BasicDataType getFullLimitType() {
        return fullLimitType;
    }

    public int getDefaultInitRate() {
        return defaultInitRate;
    }

    public static BasicDataType valueOf(int id){
        return EnumUtils.getEnum(BasicDataType.class,id);
    }
}
