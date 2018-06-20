package com.peony.engine.framework.control.statistics;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.tool.helper.BeanHelper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Service(init = "init")
public class StatisticsService {
    private Map<String, Method> dataMap = new HashMap<>();
    public void init(){
        dataMap = ServiceHelper.getStatisticsMethods();

//        for(Map.Entry<String, Method> entry : listMap.entrySet()){
//            Map<Method,Object> map = dataMap.get(entry.getKey());
//            if(map == null){
//                map = new HashMap<>();
//                dataMap.put(entry.getKey(),map);
//            }
//            Object service = BeanHelper.getServiceBean(entry.getValue().getDeclaringClass());
//            map.put(entry.getValue(),service);
//        }

    }

    public Map<String,String> getTabMap(){
        Map<String,String> map = new HashMap<>();
        for(Map.Entry<String, Method> entry: dataMap.entrySet()){
            Statistics statistics = entry.getValue().getAnnotation(Statistics.class);
            map.put(entry.getKey(),statistics.name());
        }
        return map;
    }

    public StatisticsData getData(String key){
        Method method = dataMap.get(key);
        if(method == null){
            return null;
        }
        StatisticsData ret = new StatisticsData();
        Object service = BeanHelper.getServiceBean(method.getDeclaringClass());
        try {
            ret = (StatisticsData) (method.invoke(service));
        }catch (Throwable e){
            e.printStackTrace();
        }
        return ret;
    }
}
