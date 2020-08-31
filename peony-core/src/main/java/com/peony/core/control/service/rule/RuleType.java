package com.peony.core.control.service.rule;

import com.peony.common.exception.MMException;

/**
 * @Author: zhengyuzhen
 * @Date: 2019/4/26 5:16 PM
 */
public enum RuleType {
    /**
     * 1）顺序调用【order】：先调用第一个，如果失败，则调用第二个，以此类推。容错
     #       2）随机调用【random】：从配置的服务器中随机选出一个，调用，调用失败将重试。调用平均分布，负载平衡
     #       3）取模调用【modulus】：根据自己服务器的id对配置该service的服务器个数取模i，调用第i个服务器。
     #       4）自定义规则：继承自ServiceCallRule，根据id获取调用id
     */
    Order("order"),
    Random("random"),
    Modulus("modulus"),
    SelfDefined(""),
    ;
    final String id;
    RuleType(String id){
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static RuleType getRuleTypeByConfig(String callRuleConfig){
        switch (callRuleConfig){
            case "order":
                return Order;
            case "random":
                return Random;
            case "modulus":
                return Modulus;
        }
//        if(serviceCallRuleMap.containsKey(callRuleConfig)){
//            return SelfDefined;
//        }
        throw new MMException("rule type error!,config="+callRuleConfig);
    }
}
