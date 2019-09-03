package com.peony.engine.framework.data.cache;

import com.peony.engine.framework.data.persistence.orm.EntityHelper;
import com.peony.engine.framework.data.persistence.orm.MMMethod;
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
//    public static final String SEPARATOR = "_"; // new String(new char[]{255});//
    public static final char SEPARATOR = '_';

//    private static Map<Class<?>,List<String>> pkMap = new HashMap<>();
    static {
        // 要校验所有的DBEntity，确保Class.getName()不能有一样的
    }
    // TODO 这个方法用的比较多，可以考虑用增加字节码的方式给对象添加函数，来获取key，而不是用invoke
    public static String parseKey(Object entity){
        Class<?> cls = entity.getClass();
        Map<String, MMMethod> pkMethodMap = EntityHelper.getPkGetMethodMap(cls);
        if(pkMethodMap.size() == 0){
            throw new MMException("没找到主键方法"+cls.getName());
        }
        StringBuilder sb = new StringBuilder(cls.getName());

        try {
            for(MMMethod method : pkMethodMap.values()){
                appendWithTrans(sb,parseParamToString(method.invoke(entity)),SEPARATOR);
            }

        } catch (Exception e) {
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
        Map<String,MMMethod> pkMethodMap = EntityHelper.getPkGetMethodMap(entityClass);

        if(pkMethodMap == null || pkMethodMap.isEmpty()) {
            throw new MMException("没找到主键方法 "+entityClass.getName());
        }

        Set<String> pks = pkMethodMap.keySet(); // 注意这里面的排序
//        String resultCondition = parseParamsToString(condition,params);
        String resultCondition = condition;
        String[] conditions = resultCondition.split("\\s{1,}and\\s{1,}");
        Map<String,String> pksInConditions = new HashMap<>();
        int paramsIndex = 0;
        for (String conditionStr:conditions) {
            conditionStr = conditionStr.trim();
            if(conditionStr.length() > 0){
                String[] pk = conditionStr.split("=");
                if(pk.length!=2){
                    throw new MMException("condition 参数错误,resultCondition = "+resultCondition);
                }
                String value = pk[1].trim();
                value = checkAndReplaceMark(value,paramsIndex,params);
                paramsIndex++;
                pksInConditions.put(pk[0].trim(),value);
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
            appendWithTrans(sb,pksInConditions.get(pk),SEPARATOR);
//            sb.append(SEPARATOR+pksInConditions.get(pk));
        }
        return sb.toString();
    }

    // 判断一个对象是否属于一个list
    public static <T> boolean isObjectBelongToList(Object object,String listKey){
        if(!listKey.startsWith(object.getClass().getName())){ // 是否是同一中类
            return false;
        }
        String[] listKeyStrs = listKey.split(KeyParser.LISTSEPARATOR,4); // 类，list，field，value
        if(listKeyStrs.length == 2){ // 说明是整个表的list
            return true;
        }
        if(listKeyStrs.length<4){
            log.warn("listKey is Illegal : listKey = "+listKey);
            return false;
        }
//        String[] fieldNames = listKeyStrs[2].split(SEPARATOR);
        List<String> fieldNames = split(listKeyStrs[2],SEPARATOR);
//        String[] fieldValues = listKeyStrs[3].split(SEPARATOR);
        List<String> fieldValues = split(listKeyStrs[3],SEPARATOR);
        if(fieldNames.size() !=fieldValues.size()){
            log.warn("listKey is Illegal : listKey = "+listKey);
            return false;
        }
        Map<String,MMMethod> getMethodMap = EntityHelper.getGetMethodMap(object.getClass());
        try {
            int i=0;
            for(String fieldName : fieldNames){
                MMMethod method = getMethodMap.get(fieldName);
                if(method == null){
                    log.warn("listKey is Illegal : fieldName is not exist in getMethodMap , fieldName = "+fieldName);
                    return false;
                }
                Object ret = method.invoke(object);
                if(!parseParamToString(ret).equals(fieldValues.get(i))){
                    return false;
                }
                i++;
            }
        } catch (Exception e) {
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
//        String resultCondition = parseParamsToString(condition,params);
        String resultCondition = condition;
        String[] conditions = resultCondition.split("\\s{1,}and\\s{1,}");
        //拼接key
        StringBuilder sb = new StringBuilder(entityClass.getName()+"#list");
        StringBuilder conditionNames = null,conditionValues = null;
//        Map<String,String> pksInConditions = new HashMap<>();
        int paramsIndex = 0;
        for (String conditionStr:conditions) {
            conditionStr = conditionStr.trim();
            if(conditionStr.length() > 0){
                String[] pk = conditionStr.split("=");
                if(pk.length!=2){
                    throw new MMException("condition 参数错误,resultCondition = "+resultCondition);
                }
                if(conditionNames == null){
                    conditionNames = new StringBuilder(LISTSEPARATOR);
                    appendWithTransWithoutAppendSp(conditionNames,pk[0].trim(),SEPARATOR);
                }else{
                    appendWithTrans(conditionNames,pk[0].trim(),SEPARATOR);
//                    conditionNames.append(SEPARATOR+(pk[0].trim()));
                }
                String value = pk[1].trim();
                value = checkAndReplaceMark(value,paramsIndex,params);
                paramsIndex++;
                if(conditionValues == null){
                    conditionValues = new StringBuilder(LISTSEPARATOR);
                    appendWithTransWithoutAppendSp(conditionValues,value,SEPARATOR);
                }else{
                    appendWithTrans(conditionValues,value,SEPARATOR);
//                    conditionValues.append(SEPARATOR+value);
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
    //
    private static String checkAndReplaceMark(String value,int paramsIndex,Object... params){
        if(value.equals("?")){
            if(params.length < paramsIndex+1){
                throw new MMException("params 参数错误，参数太少");
            }
            value = parseParamToString(params[paramsIndex]);
        }else {
            throw new MMException("condition 错误，请把参数值放在params中，condition中用?替代");
        }
        return value;
    }
    //
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

    /**
     * 对sp进行转义后连接
     * @param strs
     * @param sp
     * @return
     */
    public static String concat(String[] strs,char sp){
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for(String str:strs){
            if(index++>0){
                sb.append(sp);
            }
            appendWithTransWithoutAppendSp(sb,str,sp);
        }
        return sb.toString();
    }

    public static void appendWithTransWithoutAppendSp(StringBuilder sb,String str,char sp){
        for(char ch : str.toCharArray()){
            if(ch == sp || ch == '\\'){
                sb.append('\\');
            }
            sb.append(ch);
        }
    }

    public static void appendWithTrans(StringBuilder sb,String str,char sp){
        sb.append(sp);
        appendWithTransWithoutAppendSp(sb, str, sp);
    }

    /**
     * 切割考虑对sp的转义
     * @param str
     * @param sp
     * @return
     */
    public static List<String> split(String str ,char sp){
        List<String> list = new ArrayList<>();
        char[] chars = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        for(int i=0,len=chars.length;i<len;i++){
            if(chars[i] == '\\'){
                if(chars[i+1] == '\\'){
                    sb.append('\\');
                    i++;
                }else if(chars[i+1] == sp){
                    sb.append(sp);
                    i++;
                }
            }else if(chars[i] == sp){
                list.add(sb.toString());
                sb = new StringBuilder();
            }else{
                sb.append(chars[i]);
            }
        }
        list.add(sb.toString());
        return list;
    }
}
