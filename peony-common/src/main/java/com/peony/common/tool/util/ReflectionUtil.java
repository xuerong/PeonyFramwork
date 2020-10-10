package com.peony.common.tool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Administrator on 2015/11/13.
 * 反射工具
 */
public final class ReflectionUtil {
    private static final Logger log = LoggerFactory.getLogger(ReflectionUtil.class);

    /**
     * 创建实例
     * **/
    public static <T> T newInstance(Class<T> cls){
        T instance=null;
        try {
            instance= cls.newInstance();
        }catch (Exception e){
            log.error("new instance failure",e);
            throw  new RuntimeException(e);
        }
        return instance;
    }

    public static Class[] getParamsTypes(Object... params){
        if(params == null || params.length == 0){
            return null;
        }
        Class[] result = new Class[params.length];
        for(int i = 0;i<params.length;i++){
            result[i] = params[i].getClass();
            if(result[i].equals(Integer.class)){
                result[i] = int.class;
            }else if(result[i].equals(Boolean.class)){
                result[i] = boolean.class;
            }else if(result[i].equals(Long.class)){
                result[i] = long.class;
            }else if(result[i].equals(Double.class)){
                result[i] = double.class;
            }else if(result[i].equals(Float.class)){
                result[i] = float.class;
            }
        }
        return result;
    }

    /**
     * 获取本类声明的方法和包括父类在内的公有方法
     *
     * @param cls
     * @return
     */
    public static Method[] getPublicAndDeclaredMethod(Class<?> cls){
        List<Method> methodList = new ArrayList<>();
        for(Method method : cls.getDeclaredMethods()){
            methodList.add(method);
        }
        for(Method method : cls.getMethods()){
            methodList.add(method);
        }
        return methodList.toArray(new Method[methodList.size()]);
    }
}
