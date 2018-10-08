package com.peony.engine.framework.data.cache;

import com.peony.engine.framework.data.persistence.orm.EntityHelper;
import com.peony.engine.framework.security.exception.MMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by a on 2016/8/10.
 * 完成对cache的key的生成等操作
 */
public class KeyParser {
    private static final Logger log = LoggerFactory.getLogger(KeyParser.class);
    public static final String LISTSEPARATOR ="#";
    public static final String SEPARATOR ="_";

//    private static Map<Class<?>,List<String>> pkMap = new HashMap<>();
    static {
        // 要校验所有的DBEntity，确保Class.getName()不能有一样的
    }
    // TODO 这个方法用的比较多，可以考虑用增加字节码的方式给对象添加函数，来获取key，而不是用invoke
    public static String parseKey(Object entity){
        Class<?> cls = entity.getClass();
        Map<String,Method> pkMethodMap = EntityHelper.getPkGetMethodMap(cls);
        if(pkMethodMap.size() == 0){
            throw new MMException("没找到主键方法"+cls.getName());
        }
        StringBuilder sb = new StringBuilder(cls.getName());

        try {
            for(Method method : pkMethodMap.values()){
                sb.append("_"+parseParamToString(method.invoke(entity)));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 传进来的条件必须是主键
     * @return
     */
    public static String parseKeyForObject(Class<?> entityClass, String condition, Object... params){
        if(condition == null || condition.length() == 0){
            return entityClass.getName();
        }
        // 判断条件中的主键
        Map<String,Method> pkMethodMap = EntityHelper.getPkGetMethodMap(entityClass);

        if(pkMethodMap == null || pkMethodMap.isEmpty()) {
            throw new MMException("没找到主键方法 "+entityClass.getName());
        }

        Set<String> pks = pkMethodMap.keySet(); // 注意这里面的排序
        String resultCondition = parseParamsToString(condition,params);
        String[] conditions = resultCondition.split("and");
        Map<String,String> pksInConditions = new HashMap<>();
        for (String conditionStr:conditions) {
            conditionStr = conditionStr.trim();
            if(conditionStr.length() > 0){
                String[] pk = conditionStr.split("=");
                if(pk.length!=2){
                    throw new MMException("condition 参数错误,resultCondition = "+resultCondition);
                }
                pksInConditions.put(pk[0].trim(),pk[1].trim());
            }
        }
        if(pks.size() > pksInConditions.size()){
            throw new MMException("condition 参数错误,主键数量，resultCondition = "+resultCondition);
        }
        //拼接key
        StringBuilder sb = new StringBuilder(entityClass.getName());
        for (String pk:pks) {
            String pkStr = pksInConditions.get(pk);
            if(pkStr == null){
                throw new MMException("condition 参数错误,缺少主键["+pk+"]，resultCondition = "+resultCondition+",class = "+entityClass.getName());
            }
            sb.append("_"+pksInConditions.get(pk));
        }

        return sb.toString();
    }
    // 从listKey中获取对应的class的名字
    public static String getClassNameFromListKey(String listKey){
        return listKey.split("#")[0];
    }
    // 判断一个对象是否属于一个list
    public static <T> boolean isObjectBelongToList(Object object,String listKey){
        if(!listKey.startsWith(object.getClass().getName())){ // 是否是同一中类
            return false;
        }
        String[] listKeyStrs = listKey.split(LISTSEPARATOR);
        if(listKeyStrs.length == 2){ // 说明是整个表的list
            return true;
        }
        if(listKeyStrs.length<4){
            log.warn("listKey is Illegal : listKey = "+listKey);
            return false;
        }
        String[] fieldNames = listKeyStrs[2].split(SEPARATOR);
        String[] fieldValues = listKeyStrs[3].split(SEPARATOR);
        if(fieldNames.length !=fieldValues.length){
            log.warn("listKey is Illegal : listKey = "+listKey);
            return false;
        }
        Map<String,Method> getMethodMap = EntityHelper.getGetMethodMap(object.getClass());
        try {
            int i=0;
            for(String fieldName : fieldNames){
                Method method = getMethodMap.get(fieldName);
                if(method == null){
                    log.warn("listKey is Illegal : fieldName is not exist in getMethodMap , fieldName = "+fieldName);
                    return false;
                }
                Object ret = method.invoke(object);
                if(!parseParamToString(ret).equals(fieldValues[i])){
                    return false;
                }
                i++;
            }
        } catch (IllegalAccessException |InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * list的key如何实际：
     * entityClass.getName()#list#[条件列名_...]#[条件值_...]
     * LISTSEPARATOR
     * @param entityClass
     * @param condition
     * @param params
     * @return
     */
    public static String parseKeyForList(Class<?> entityClass, String condition, Object... params){
        if(condition == null || condition.length()==0){
            return entityClass.getName()+"#list";
        }
        String resultCondition = parseParamsToString(condition,params);
        String[] conditions = resultCondition.split("and");
        //拼接key
        StringBuilder sb = new StringBuilder(entityClass.getName()+"#list");
        StringBuilder conditionNames = null,conditionValues = null;
        Map<String,String> pksInConditions = new HashMap<>();
        for (String conditionStr:conditions) {
            conditionStr = conditionStr.trim();
            if(conditionStr.length() > 0){
                String[] pk = conditionStr.split("=");
                if(pk.length!=2){
                    throw new MMException("condition 参数错误,resultCondition = "+resultCondition);
                }
                if(conditionNames == null){
                    conditionNames = new StringBuilder("#"+pk[0].trim());
                }else{
                    conditionNames.append("_"+(pk[0].trim()));
                }
                if(conditionValues == null){
                    conditionValues = new StringBuilder("#"+pk[1].trim());
                }else{
                    conditionValues.append("_"+(pk[1].trim()));
                }
            }
        }
        if(conditionNames != null) {
            sb.append(conditionNames).append(conditionValues);
        }
        log.debug("get condition:{}", sb.toString());
        return sb.toString();
    }
    ///////---------------------------工具----------------------
    private static String parseParamsToString(String condition, Object... params){
        if(params.length == 0 || condition.indexOf('?') == -1){
            return condition;
        }
        if(params.length>=1 && condition.indexOf('?') == condition.lastIndexOf('?')){
            return condition.replace("?",parseParamToString(params[0]));
        }
        StringBuilder sb = new StringBuilder(condition);
        int count = 0;
        int paramsCount = 0;
        String paramsStr = null;
        for (char c: condition.toCharArray()){
            if(c == '?'){
                if(params.length <= paramsCount){
                    throw new MMException("params 参数错误，参数太少");
                }
                paramsStr = parseParamToString(params[paramsCount]);
                sb.replace(count,count+1,paramsStr);
                paramsCount++;
                count+=(paramsStr.length()-1);
            }
            count++;
        }
        return sb.toString();
    }
    // TODO 这个地方得看看人家是怎么用的
    public static String parseParamToString(Object param){
        if(param instanceof Timestamp){
            return ((Timestamp)param).getTime()+"";
        }
        if(param == null){
            log.error("select params contain null");
            throw new MMException("select params contain null");
        }
        return param.toString();
    }
}
