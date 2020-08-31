package com.peony.demo.assembly.battle.round.hero;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;


/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-15 14:37
 */
@Slf4j
@Data
@NotThreadSafe
public class BasicData {

    Map<BasicDataType,Long> values; //

    public BasicData(Map<BasicDataType,Long> values){
        this.values = values;
    }

    public long get(BasicDataType basicDataType){
        Long value = values.get(basicDataType);
        return value==null?0:value;
    }

    public Map<BasicDataType,Long> initDefaultData(){
        Map<BasicDataType,Long> ret = new HashMap<>();
        for(BasicDataType basicDataType:BasicDataType.values()){
            if(basicDataType.getFullLimitType() != null){
                Long value =  values.get(basicDataType);
                if(value == null){
                    Long fullValue =  values.get(basicDataType.getFullLimitType());
                    if(fullValue != null){
                        long newValue = fullValue*basicDataType.getDefaultInitRate()/100;
                        values.put(basicDataType,newValue);
                        ret.put(basicDataType,newValue);
                    }
                }
            }
        }
        return ret;
    }

    public long changeBasicData(BasicDataType basicDataType,long value){
        Long oriValue = values.get(basicDataType);
        if(oriValue == null){
            oriValue = 0l;
        }
        oriValue += value;
        if(oriValue<0 && !basicDataType.isMayBeNegative()){
            oriValue = 0l;
        }
        // 满限制
        BasicDataType fullLimitType = basicDataType.getFullLimitType();
        if(fullLimitType != null){
            Long fullLimitValue = values.get(fullLimitType);
            if(fullLimitValue != null && oriValue > fullLimitValue){
                oriValue = fullLimitValue;
            }
        }
        //
        values.put(basicDataType,oriValue);
        return oriValue;
    }

    public JSONObject toJson(){
        JSONObject ret = new JSONObject();
        for(Map.Entry<BasicDataType,Long> entry:values.entrySet()){
            ret.put(String.valueOf(entry.getKey().getId()),entry.getValue());
        }
        return ret;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<BasicDataType,Long> entry:values.entrySet()){
            sb.append("[").append(entry.getKey()).append(":").append(entry.getValue()).append("]");
        }
        return sb.toString();
    }
}
