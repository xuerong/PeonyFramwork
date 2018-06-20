package com.peony.engine.framework.control.aop;

import com.peony.engine.framework.control.aop.annotation.Aspect;
import com.peony.engine.framework.control.aop.annotation.AspectOrder;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.ClassHelper;
import com.peony.engine.framework.tool.util.ReflectionUtil;
import net.sf.cglib.proxy.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Administrator on 2015/11/17.
 * 初始化时，将所有的类变成其对应的代理类
 */
public final class AopHelper {
    private static Map<Class<?>,List<Proxy>> targetMap;
    static{
        try {
            //将所有的类变成其对应的代理类
            //获取所有类-方法及其代理关系

            // 创建 Proxy Map（用于 存放代理类 与 目标类列表 的映射关系）
            Map<Class<?>, List<Class<?>>> proxyMap = createProxyMap();
            // 创建 Target Map（用于 存放目标类 与 代理类列表 的映射关系）
            targetMap=createTargetMap(proxyMap);

        } catch (Exception e) {

            throw new RuntimeException("初始化 AopHelper 出错！", e);
        }
    }
    /**
     * 传入一个类，返回其对应的代理对象
     *
     * 由于初始化AopHelper的时候，是根据实际已经拥有的类——在相应包下的——进行对代理判断的
     * 如果某个类在此之外被代理，那么就要对被代理之后的心类添加aop，并实例化之
     * **/
    public static <T> T getProxyObject(Class<?> keyCls,Class<T> newCls){
        List<Proxy> proxyList = targetMap.get(keyCls);
        if(proxyList != null){
            return createProxyObject(newCls, proxyList);
        }
        return ReflectionUtil.newInstance(newCls);
    }
    public static <T> T getProxyObject(Class<T> cls){
        List<Proxy> proxyList = targetMap.get(cls);
        if(proxyList != null){
            return createProxyObject(cls, proxyList);
        }
        return ReflectionUtil.newInstance(cls);
    }
    /**
     * 给一个cls添加对应的代理列表，并实例化之
     * proxyList：
     *  这里面存储的是该类的代理类，每个代理类并不一定是对所有的方法进行代理
     * 在前面赛选目标类和对应的代理类的时候，就要将对应的代理类是否代理该目标类中的所有方法标识出来，用来赛选方法
     * */
    private static <T> T createProxyObject(final Class<?> target,final List<Proxy> proxyList){
        Enhancer enhancer=new Enhancer();
        enhancer.setSuperclass(target);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object targetObject, Method method, Object[] methodParams, MethodProxy methodProxy) throws Throwable {
                for (Proxy proxy :proxyList) {
                    if(proxy.executeMethod(method)){
                        try {
                            proxy.before(targetObject, target, method, methodParams);
                        }catch (Throwable e){
                            e.printStackTrace();
                        }
                    }
                }
                int size=proxyList.size();

                Object result = null;
                try {
                    result = methodProxy.invokeSuper(targetObject, methodParams);
                } catch (Throwable e) {
                    // 执行exception方法
                    for (int i = size - 1; i >= 0; i--) {
                        Proxy proxy = proxyList.get(i);
                        if (proxy.executeMethod(method)) {
                            try {
                                proxy.exceptionCatch(e);
                            } catch (Throwable e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                    throw e;
                }
                // 执行after方法
                for (int i = size - 1; i >= 0; i--) {
                    Proxy proxy = proxyList.get(i);
                    if (proxy.executeMethod(method)) {
                        try {
                            proxy.after(targetObject, target, method, methodParams, result);
                        } catch (Throwable e) {
                            if(e instanceof MMException){
                                MMException exception = (MMException)e;
                                if(exception.getExceptionType() == MMException.ExceptionType.TxCommitFail){
                                    throw e;
                                }
                            }
                        }
                    }
                }
                return result;
            }
        });//NoOp.INSTANCE
        return (T)enhancer.create();
    }

    private static Map<Class<?>, List<Class<?>>> createProxyMap() throws Exception {
        Map<Class<?>, List<Class<?>>> proxyMap = new LinkedHashMap<Class<?>, List<Class<?>>>();
        // 获取切面类（所有继承于 BaseAspect 的类）
        List<Class<?>> aspectProxyClassList = ClassHelper.getClassListBySuper(AspectProxy.class);
        // 排序切面类
        sortAspectProxyClassList(aspectProxyClassList);
        // 遍历切面类
        for (Class<?> aspectProxyClass : aspectProxyClassList) {
            // 判断 Aspect 注解是否存在
            if (aspectProxyClass.isAnnotationPresent(Aspect.class)) {
                // 获取 Aspect 注解
                Aspect aspect = aspectProxyClass.getAnnotation(Aspect.class);
                // 创建目标类列表
                List<Class<?>> targetClassList = createTargetClassList(aspect);
                // 初始化 Proxy Map
                proxyMap.put(aspectProxyClass, targetClassList);
            }
        }
        return proxyMap;
    }
    private static boolean isCross(String[] strs1,String[] strs2){
        for (String str1 :strs1) {
            for (String str2 :strs2) {
                if(str1.equals(str2)){
                    return true;
                }
            }
        }
        return false;
    }
    private static List<Class<?>> createTargetClassList(Aspect aspect) throws Exception {
        List<Class<?>> targetClassList = new ArrayList<Class<?>>();

        List<Class<?>> allClass=ClassHelper.getClassList();
        for (Class<?> cls : allClass) {
            // 是否是定义的包中的
            if(AopUtil.isExecuteClass(cls,aspect)){
                targetClassList.add(cls);
            }
        }
        return targetClassList;
    }
    // 排序代理类
    private static void sortAspectProxyClassList(List<Class<?>> proxyClassList) {
        // 排序代理类列表
        Collections.sort(proxyClassList, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> aspect1, Class<?> aspect2) {
                if (aspect1.isAnnotationPresent(AspectOrder.class) || aspect2.isAnnotationPresent(AspectOrder.class)) {
                    // 若有 Order 注解，则优先比较（序号的值越小越靠前）
                    if (aspect1.isAnnotationPresent(AspectOrder.class)) {
                        return getOrderValue(aspect1) - getOrderValue(aspect2);
                    } else {
                        return getOrderValue(aspect2) - getOrderValue(aspect1);
                    }
                } else {
                    // 若无 Order 注解，则比较类名（按字母顺序升序排列）
                    return aspect1.hashCode() - aspect2.hashCode();
                }
            }

            private int getOrderValue(Class<?> aspect) {
                return aspect.getAnnotation(AspectOrder.class) != null ? aspect.getAnnotation(AspectOrder.class).value() : 0;
            }
        });
    }
    private static Map<Class<?>, List<Proxy>> createTargetMap(Map<Class<?>, List<Class<?>>> proxyMap) throws Exception {
        Map<Class<?>, List<Proxy>> targetMap = new HashMap<Class<?>, List<Proxy>>();
        // 遍历 Proxy Map
        for (Map.Entry<Class<?>, List<Class<?>>> proxyEntry : proxyMap.entrySet()) {
            // 分别获取 map 中的 key 与 value
            Class<?> proxyClass = proxyEntry.getKey();
            List<Class<?>> targetClassList = proxyEntry.getValue();
            // 遍历目标类列表
            for (Class<?> targetClass : targetClassList) {
                // 创建代理类（切面类）实例，即，每个目标类中都有单独的其代理类的实例
                Proxy baseAspect = (Proxy) proxyClass.newInstance();
                baseAspect.setTargetClass(targetClass);
                // 初始化 Target Map
                if (targetMap.containsKey(targetClass)) {
                    targetMap.get(targetClass).add(baseAspect);
                } else {
                    List<Proxy> baseAspectList = new ArrayList<Proxy>();
                    baseAspectList.add(baseAspect);
                    targetMap.put(targetClass, baseAspectList);
                }
            }
        }
        return targetMap;
    }
}
