package com.peony.core.control.aop;

import com.peony.core.control.ServiceHelper;
import com.peony.core.control.aop.annotation.Aspect;
import com.peony.core.control.aop.annotation.AspectOrder;
import com.peony.common.exception.MMException;
import com.peony.common.tool.helper.ClassHelper;
import com.peony.common.tool.util.ReflectionUtil;
import com.peony.core.server.Server;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.bytecode.AnnotationsAttribute;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 实现peony中AOP功能的主类。通过cglib的代理类技术来对需要的Service类添加代理类
 * 来实现AOP功能。
 *
 * @author zhengyuzhen
 * @see AspectProxy
 * @see Aspect
 * @see AspectOrder
 * @since 1.0
 */
public final class AopHelper {
    private static final Logger log = LoggerFactory.getLogger(AopHelper.class);

    /**
     * 切点类-切面类列表
     */
    private static Map<Class<?>, List<Proxy>> targetMap;

    static {
        try {
            // 创建 Proxy Map（用于 存放切面类 与 切点类列表 的映射关系）
            Map<Class<?>, List<Class<?>>> proxyMap = createProxyMap();
            // 创建 Target Map（用于 存放切点类 与 切面类列表 的映射关系）
            targetMap = createTargetMap(proxyMap);
        } catch (Exception e) {
            throw new RuntimeException("初始化 AopHelper 出错！", e);
        }
    }

    /**
     * 传入一个类，返回其对应的代理对象。
     * <p>
     * 由于初始化AopHelper的时候，是根据实际已经拥有的类——在相应包下的——进行对代理判断的
     * 如果某个类在此之外被代理，那么就要对被代理之后的心类添加aop，并实例化之
     *
     * @param keyCls 需要被代理的类的原类
     * @param newCls 之前被代理过的类
     * @param <T>    被代理的类的实例
     * @return 代理之后的类
     */
    public static <T> T getProxyObject(Class<?> keyCls, Class<T> newCls) {
        List<Proxy> proxyList = targetMap.get(keyCls);
        if (proxyList != null) {
            return createProxyObject(newCls, proxyList);
        }
        return ReflectionUtil.newInstance(newCls);
    }

    /**
     * 传入一个类，返回其对应的代理对象。
     *
     * @param cls 需要被代理的类
     * @param <T> 被代理的类的实例
     * @return 代理之后的类的实例
     */
    public static <T> T getProxyObject(Class<T> cls) {
        List<Proxy> proxyList = targetMap.get(cls);
        if (proxyList != null) {
            return createProxyObject(cls, proxyList);
        }
        return ReflectionUtil.newInstance(cls);
    }

    /**
     * 给一个代类添加对应的代理列表，并实例化之。
     *
     * @param target    被代理的类
     * @param proxyList 该类的代理类，每个代理类并不一定是对所有的方法进行代理
     * @param <T>       被代理的类的实例
     * @return 代理之后的类
     */
    private static <T> T createProxyObject(final Class<?> target, final List<Proxy> proxyList) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(target);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object targetObject, Method method, Object[] methodParams, MethodProxy methodProxy) throws Throwable {
                int txTimes = 0;
                while (true) {
                    // 执行所有切面的before方法
                    for (Proxy proxy : proxyList) {
                        if (proxy.executeMethod(method)) {
                            try {
                                proxy.before(targetObject, target, method, methodParams);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    int size = proxyList.size();

                    // 执行切点方法
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
                                    log.error("代理类异常处理异常", e2);
                                }
                            }
                        }
                        throw e;
                    }
                    // 执行所有切面的after方法
                    MMException txException = null;
                    for (int i = size - 1; i >= 0; i--) {
                        Proxy proxy = proxyList.get(i);
                        if (proxy.executeMethod(method)) {
                            try {
                                proxy.after(targetObject, target, method, methodParams, result);
                            } catch (Throwable e) {
                                if (e instanceof MMException) {
                                    MMException exception = (MMException) e;
                                    if (exception.getExceptionType() == MMException.ExceptionType.TxCommitFail) {
                                        txException = exception;
                                    }
                                } else {
                                    log.error("aop execute after error!", e);
                                }
                            }
                        }
                    }
                    /**
                     * 如果是事务提交异常，将给予重试机会，直到达到限定次数。
                     * 这可以提高在高并发情况下，加锁校验实现的事务的一致性在校验失败时事务最终执行成功的概率。
                     */
                    if (txException != null) {
                        if (++txTimes >= 5) {
                            log.error("tx fail times {} ,fail!", txTimes);
                            throw txException;
                        } else {
                            log.warn("tx fail times {} ,exec again", txTimes);
                            continue;
                        }
                    }
                    return result;
                }
            }
        });//NoOp.INSTANCE
        return (T) enhancer.create();
    }

    private static Map<String, ProxyExecInfo> proxyMap = new HashMap<>();

    static class ProxyExecInfo {
        Class<?> target;
        Method method;
        List<Proxy> proxyList = new ArrayList<>();
    }

    public static void execProxyBefore(String id, Object targetObject, Object[] methodParams) {
        ProxyExecInfo proxyExecInfo = proxyMap.get(id);
        for (Proxy proxy : proxyExecInfo.proxyList) {
            try {
                proxy.before(targetObject, proxyExecInfo.target, proxyExecInfo.method, methodParams);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean execProxyAfter(String id, Object targetObject, Object[] methodParams, Object result, final int txTimes) {
        MMException txException = null;
        ProxyExecInfo proxyExecInfo = proxyMap.get(id);
        for (Proxy proxy : proxyExecInfo.proxyList) {
            try {
                proxy.after(targetObject, proxyExecInfo.target, proxyExecInfo.method, methodParams, result);
            } catch (Throwable e) {
                if (e instanceof MMException) {
                    MMException exception = (MMException) e;
                    if (exception.getExceptionType() == MMException.ExceptionType.TxCommitFail) {
                        txException = exception;
                    }
                } else {
                    log.error("aop execute after error!", e);
                }
            }
        }
        /**
         * 如果是事务提交异常，将给予重试机会，直到达到限定次数。
         * 这可以提高在高并发情况下，加锁校验实现的事务的一致性在校验失败时事务最终执行成功的概率。
         */
        if (txException != null) {
            if (txTimes >= 5) {
                log.error("tx fail times {} ,fail!", txTimes);
                throw txException;
            } else {
                log.warn("tx fail times {} ,exec again", txTimes);
                return false;
            }
        }
        return true;
    }

    /**
     * 给一个代类添加对应的代理列表，并实例化之。
     *
     * @param target    被代理的类
     * @param proxyList 该类的代理类，每个代理类并不一定是对所有的方法进行代理
     * @param <T>       被代理的类的实例
     * @return 代理之后的类
     */
    private static <T> T createProxyObject2(final Class<?> target, final List<Proxy> proxyList) throws Exception {
        //
        ClassPool pool = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader());
        CtClass ctClazz = pool.get(target.getName());
        CtClass proxyClazz = pool.makeClass(ctClazz.getName() + "$AOPProxy", ctClazz);
        //
        Method[] methods = ReflectionUtil.getPublicAndDeclaredMethod(target);
        for (Method method : methods) {
            String id = UUID.randomUUID().toString();
            for (Proxy proxy : proxyList) {
                if (proxy.executeMethod(method)) {
                    ProxyExecInfo proxyExecInfo = proxyMap.get(id);
                    if (proxyExecInfo == null) {
                        proxyExecInfo = new ProxyExecInfo();
                        // TODO 这个地方不一定对，可能需要的是代理之后的类和方法对象
                        proxyExecInfo.target = target;
                        proxyExecInfo.method = method;
                        proxyMap.put(id, proxyExecInfo);
                    }
                    proxyExecInfo.proxyList.add(proxy);
                }
            }
            if (!proxyMap.containsKey(id)) {
                // 不需要被代理
                continue;
            }
            CtMethod ctMethod = ctClazz.getMethod(method.getName(), ServiceHelper.packDescreptor(method));
            CtMethod proxyMethod = CtNewMethod.copy(ctMethod, proxyClazz, null);
            for (Object attr : ctMethod.getMethodInfo().getAttributes()) {
                if (attr.getClass().isAssignableFrom(AnnotationsAttribute.class)) {
                    AnnotationsAttribute attribute = (AnnotationsAttribute) attr;
                    proxyMethod.getMethodInfo().addAttribute(attribute);
                }
            }
            // 执行父类
            StringBuilder body = new StringBuilder();
            String argsString = ServiceHelper.genArgsString(method);

            body.append("{\n");
            // for循环，用于事务的支持
            body.append("int txTimes = 0;");
            body.append("while (true) {");
            // 插入before
            body.append("com.peony.core.control.aop.AopHelper.execProxyBefore(");
            body.append("\"").append(id).append("\",").append("this,").append("new Object[]{").append(argsString).append("}");
            body.append(");");
            // 插入before end
            if (method.getReturnType() != Void.TYPE) {
                body.append("\t\tObject ret = ");
            }
            body.append(String.format(" super.%s(%s);\n", method.getName(), argsString));
            // 插入after
            body.append("boolean goon = com.peony.core.control.aop.AopHelper.execProxyAfter(");
            body.append("\"").append(id).append("\",").append("this,").append("new Object[]{").append(argsString).append("},");
            if (method.getReturnType() != Void.TYPE) {
                body.append("ret,");
            }else{
                body.append("null,");
            }
            body.append("txTimes++");
            body.append(");");

            body.append("if(!goon){");
            body.append("continue;");
            body.append("}");
            // 插入after end
            if (method.getReturnType() != Void.TYPE) {
                body.append("\t\treturn ret;");
            }else{
                body.append("\t\treturn;");
            }
            body.append("}");
            body.append(String.format("\t}"));

            proxyMethod.setBody(body.toString());
            proxyClazz.addMethod(proxyMethod);
        }
        Class<?> newServiceClass = proxyClazz.toClass();
//        newServiceClass.newInstance()
        return (T)newServiceClass;
    }


    /**
     * 获取切面类和切点类的对应关系。
     *
     * @return 切面类-切点类列表
     */
    private static Map<Class<?>, List<Class<?>>> createProxyMap() {
        Map<Class<?>, List<Class<?>>> proxyMap = new LinkedHashMap<>();
        // 获取切面类（所有继承于 AspectProxy 的类）
        List<Class<?>> aspectProxyClassList = ClassHelper.getClassListBySuper(AspectProxy.class, Server.getEngineConfigure().getAllPackets());
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

    /**
     * 根据切面类获取对应的切点类列表。
     *
     * @param aspect 切面类
     * @return 切点类列表
     */
    private static List<Class<?>> createTargetClassList(Aspect aspect) {
        List<Class<?>> targetClassList = new ArrayList<Class<?>>();
        // 获取属于peony的所有类
        List<Class<?>> allClass = ClassHelper.getClassList(Server.getEngineConfigure().getAllPackets());
        for (Class<?> cls : allClass) {
            // 是否是定义的包中的
            if (AopUtil.isExecuteClass(cls, aspect)) {
                targetClassList.add(cls);
            }
        }
        return targetClassList;
    }

    /**
     * 根据{@link AspectOrder}对切面类排序。
     * <p>
     * 排序规则：1、没有AspectOrder注解的顺序值按照0计算，他们之间的排序按照按字母顺序升序排列；
     * 2、对于AspectOrder中顺序值相同的，不确定先后顺序
     *
     * @param proxyClassList 切面类列表
     */
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

    /**
     * 根据切面类-切点类列表，获取切点类-切面类列表。
     *
     * @param proxyMap 切面类-切点类列表
     * @return 切点类-切面类列表
     * @throws Exception
     */
    private static Map<Class<?>, List<Proxy>> createTargetMap(Map<Class<?>, List<Class<?>>> proxyMap) throws Exception {
        Map<Class<?>, List<Proxy>> targetMap = new HashMap<Class<?>, List<Proxy>>();
        // 遍历 Proxy Map
        for (Map.Entry<Class<?>, List<Class<?>>> proxyEntry : proxyMap.entrySet()) {
            // 分别获取 map 中的 key 与 value
            Class<?> proxyClass = proxyEntry.getKey();
            List<Class<?>> targetClassList = proxyEntry.getValue();
            //
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
