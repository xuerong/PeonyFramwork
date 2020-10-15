package com.peony.core.control;

import com.peony.cluster.ClusterHelper;
import com.peony.cluster.servicerole.ServiceRole;
import com.peony.core.configure.EngineConfigure;
import com.peony.core.configure.EntranceConfigure;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.aop.AopHelper;
import com.peony.core.net.entrance.Entrance;
import com.peony.common.exception.MMException;
import com.peony.core.server.Server;
import javassist.ClassPool;
import javassist.LoaderClassPath;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/11/16.
 * 存储所有的bean的实例，包括框架所用的bean，引擎服务所用的bean和用户服务所用的bean
 *
 * 注意，不设置注册bean的函数，所有的bean的初始化都在static中完成
 */
public final class BeanHelper {
    private static final Logger log = LoggerFactory.getLogger(BeanHelper.class);
    private final static Map<Class<?>,Object> frameBeans=new HashMap<Class<?>,Object>();

    private final static Map<String,Object> serviceBeansByName = new HashMap<>();
    private final static Map<Class<?>,Object> serviceBeans=new HashMap<Class<?>,Object>();
    private final static Map<String,Entrance> entranceBeans = new HashMap<>();

    public static Map<Class<?>, Object> getServiceBeans() {
        return serviceBeans;
    }

    public static Map<String, Entrance> getEntranceBeans() {
        return entranceBeans;
    }

    public static Map<Class<?>,Object> getFrameBeans() {
        return frameBeans;
    }

    /**
     *
     * 添加实例化的时候别忘了验证是否已经存在
     * */
    private static Map<Class<?>, Class<?>> configureBeans;
    static{
        synchronized (BeanHelper.class){
            ClusterHelper.init((Map<Class<?>, ServiceRole> serviceRoleMap)->{
                synchronized (BeanHelper.class){
                    //
                    for(Map.Entry<Class<?>, ServiceRole> entry : serviceRoleMap.entrySet()){
                        if(entry.getValue() == ServiceRole.None){
                            // FIXME 这里要移除
                            continue;
                        }
                        // 这里重新生成
                        try {
                            ClassPool pool = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader());
                            Class<?> newServiceClass = ServiceHelper.handleOneClass(pool, entry.getKey());
                            Object serviceObject = newAopInstance(entry.getKey(), newServiceClass);
                            serviceObject = ClusterHelper.parseService(entry.getKey(),serviceObject);
                            // 调用替换能力
                            replaceService(entry.getKey(), serviceObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            try {
                // service
                Map<Class<?>, Class<?>> serviceClassMap = ServiceHelper.getServiceClassMap();
                for (Map.Entry<Class<?>, Class<?>> entry : serviceClassMap.entrySet()) {
                    Object serviceObject = newAopInstance(entry.getKey(), entry.getValue());
                    // 添加到ClassPool中
                    ClassPool pool = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader());
                    pool.appendClassPath(new LoaderClassPath(serviceObject.getClass().getClassLoader()));
                    // 支持集群的
                    serviceObject = ClusterHelper.parseService(entry.getKey(),serviceObject);
                    if(serviceObject == null){
                        // 这里应该不会出现，因为前面已经判断完了是否为None
                        log.error("won't happened!");
                        continue;
                    }
                    //
                    serviceBeans.put(entry.getKey(), serviceObject);
                    serviceBeansByName.put(entry.getKey().getName(),serviceObject);
                }

                // 框架Bean
                EngineConfigure configure = Server.getEngineConfigure();
                configureBeans = configure.getConfigureBeans();
                for (Map.Entry<Class<?>, Class<?>> entry : configureBeans.entrySet()) {
                    // 由于在实例化一个的时候，可能用到另外一个，就在get的时候实例化了
                    // 首先看是否是service，如果是，从service中获取，否则，创建
                    if(!frameBeans.containsKey(entry.getKey())) {
                        if(entry.getValue().getAnnotation(Service.class)!=null){
                            Object object = serviceBeans.get(entry.getValue());// 注意：这里是value
                            if(object == null){
                                throw new MMException("service frameBean fail:"+entry.getValue());
                            }
                            frameBeans.put(entry.getKey(), object);
                        }else{
                            Object object = newAopInstance(entry.getValue());
                            frameBeans.put(entry.getKey(), object);
                        }
                    }
                }
                // Ioc: 如果bean中有声明的serviceBeans中存在的变量，则赋值
                iocSetService(null);
                // aop
//            frameBeans.put(MyProxyTarget.class, newAopInstance(MyProxyTarget.class));
                // net
                Map<String, EntranceConfigure> entranceClassMap = configure.getEntranceClassMap();
                for (EntranceConfigure entranceConfigure:entranceClassMap.values()) {

                    Entrance entrance = (Entrance)serviceBeans.get(entranceConfigure.getCls()) ; // 入口也可能声明为service
                    if(entrance == null){
                        entrance = (Entrance) newAopInstance(entranceConfigure.getCls());
                    }
                    Method nameMethod = Entrance.class.getMethod("setName",String.class);
                    nameMethod.invoke(entrance,entranceConfigure.getName());
                    Method portMethod = Entrance.class.getMethod("setPort",int.class);
                    portMethod.invoke(entrance,entranceConfigure.getPort());

                    entranceBeans.put(entranceConfigure.getName(),entrance);

                    //-----ioc
                    iocSetObject(entranceConfigure.getCls(),entrance, null);
                }

            }catch (Throwable e){
                log.error("init BeanHelper fail",e);
            }
        }
    }

    /**
     * 替换服务
     * TODO 基于一致性风险，是否考虑 停止当前所有服务的运行
     * 所有任务线程受控的基础上：
     * 1、采用读写锁实现
     * 2、采用令牌实现
     */
    private static void replaceService(Class<?> cls, Object serviceObject){
        if(!serviceObject.getClass().isAssignableFrom(cls)){
            log.error("object type {} is not assignable from cls {}", serviceObject.getClass().getName(), cls.getName());
            return;
        }
        //
        serviceBeans.put(cls, serviceObject);
        serviceBeansByName.put(cls.getName(),serviceObject);
        // 重新注入。包括：serviceBeans，frameBeans，entrance
        try {
            // serviceBeans，frameBeans
            iocSetService(cls);
            // entrance
            EngineConfigure configure = Server.getEngineConfigure();
            Map<String, EntranceConfigure> entranceClassMap = configure.getEntranceClassMap();
            for (EntranceConfigure entranceConfigure:entranceClassMap.values()) {
                Entrance entrance = entranceBeans.get(entranceConfigure.getName());
                iocSetObject(entranceConfigure.getCls(),entrance, cls);
            }
        } catch (Throwable throwable) {
            log.error("replace service error!", throwable);
        }
    }

    /**
     * 给beans中的对象的service变量赋值
     *
     * @param targetCls 被注入参数类型, 如果为null，说明都注入
     * @throws Throwable
     */
    private static void iocSetService(Class<?> targetCls) throws Throwable{
        // service
        for(Map.Entry<Class<?>,Object> entry : serviceBeans.entrySet()){
            Class<?> cls = entry.getKey();
            iocSetObject(cls,entry.getValue(),targetCls);
        }
        // frame
        // ioc
        for (Map.Entry<Class<?>, Class<?>> entry : configureBeans.entrySet()) {
            Object object = frameBeans.get(entry.getKey());
            iocSetObject(entry.getValue(),object, targetCls);
        }
    }

    /**
     * 为类型（或父类型）为sourceClass的object对象注入类型为targetCls的参数
     * 如果targetCls为null，说明都注入
     *
     * @param sourceClass
     * @param object
     * @param targetCls
     * @throws Throwable
     */
    private static void iocSetObject(Class sourceClass, Object object, Class<?> targetCls) throws Throwable{
        Field[] fields = sourceClass.getDeclaredFields();
        for(Field field : fields){
            if(targetCls != null && field.getClass() != targetCls){
                continue;
            }
            Object service = serviceBeans.get(field.getType());
            if(service == null){
                service = frameBeans.get(field.getType());
            }
            if(service != null){
                field.setAccessible(true); // 可访问私有变量。
                field.set(object,service);
            }
        }
    }

    // Bean类的实例化，之前需要先加aop : 目标类等于代理类
    public static <T> T newAopInstance(Class<T> cls){
        T reCls = AopHelper.getProxyObject(cls);
        return reCls;
    }
    //  : 目标类不等于代理类
    public static <T> T newAopInstance(Class<?> keyCls, Class<T> newCls){
        T reCls = AopHelper.getProxyObject(keyCls,newCls);
        return reCls;
    }

    /**
     * 获取框架所用的bean，这部分bean从配置文件中读取其实现类，或者在引擎初始化的时候设定它，使用者可以自定义之
     *
     * 由于在实例化一个的时候，可能用到另外一个，就在这里实例化了
     * **/
    public static <T> T getFrameBean(Class<T> cls){
        Object t = frameBeans.get(cls);
        if(t == null){
            Class<?> c = configureBeans.get(cls);
            if(c != null){
                t = newAopInstance(c);
                frameBeans.put(cls, t);
            }else{
                log.error("con't get frame bean by class" + cls);
            throw new RuntimeException("con't get frame bean by class" + cls);
        }
        }
        return (T)t;
    }
    /**
     * 获取ServiceBean
     * **/
    public static <T> T getServiceBean(Class<T> cls){
        Object t = serviceBeans.get(cls);
        if(t == null){
            log.error("con't get service bean by class"+cls);
            throw new RuntimeException("con't get service bean by class"+cls);
        }
        return (T)t;
    }
    /**
     * 获取ServiceBean
     * **/
    public static <T> T getServiceBean(String name){
        Object t = serviceBeansByName.get(name);
        if(t == null){
            log.error("con't get service bean by class"+name);
            throw new RuntimeException("con't get service bean by class"+name);
        }
        return (T)t;
    }

}
