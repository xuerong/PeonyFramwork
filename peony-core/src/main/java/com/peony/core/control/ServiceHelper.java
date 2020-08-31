package com.peony.core.control;

import com.google.api.client.util.Maps;
import com.peony.core.control.annotation.NetEventListener;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.annotation.Updatable;
import com.peony.core.control.event.EventListenerHandler;
import com.peony.core.control.gm.Gm;
import com.peony.core.control.netEvent.NetEventData;
import com.peony.core.control.netEvent.NetEventListenerHandler;
import com.peony.core.control.netEvent.remote.RemoteCallService;
import com.peony.core.control.request.RequestHandler;
import com.peony.core.control.rpc.IRoute;
import com.peony.core.control.rpc.Remotable;
import com.peony.core.control.rpc.RemoteExceptionHandler;
import com.peony.core.control.rpc.RouteType;
import com.peony.core.control.statistics.Statistics;
import com.peony.core.control.statistics.StatisticsData;
import com.peony.core.data.entity.session.Session;
import com.peony.core.data.persistence.orm.annotation.DBEntity;
import com.peony.core.data.tx.Tx;
import com.peony.core.security.Monitor;
import com.peony.common.exception.MMException;
import com.peony.core.server.Server;
import com.peony.common.tool.helper.ClassHelper;
import com.peony.common.tool.util.ClassUtil;
import gnu.trove.map.hash.TIntObjectHashMap;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import com.peony.core.control.annotation.EventListener;

/**
 * Created by Administrator on 2015/11/18.
 * ServiceHelper
 * 在系统启动的时候，找到所有的Service并用对应的Handler封装，
 * 然后传递给对应的管理器,分别为：
 * Request-RequestHandler-RequestService
 * EventListener-EventListenerHandler-EventService
 * NetEventListener-NetEventListenerHandler-NetEventService
 * Updatable-
 *
 *
 *
 $0, $1, $2, ...
 this and 方法的参数
 $args
 方法参数数组.它的类型为 Object[]
 $$
 所有实参。例如, m($$) 等价于 m($1,$2,...)
 $cflow(...)
 cflow 变量
 $r
 返回结果的类型，用于强制类型转换
 $w
 包装器类型，用于强制类型转换
 $_
 返回值
 $sig
 类型为 java.lang.Class 的参数类型数组
 $type
 一个 java.lang.Class 对象，表示返回值类型
 $class
 一个 java.lang.Class 对象，表示当前正在修改的类

 链接：https://www.jianshu.com/p/b9b3ff0e1bf8
 */
public final class ServiceHelper {
    private static final Logger log = LoggerFactory.getLogger(ServiceHelper.class);

    private static TIntObjectHashMap<Class<?>> requestHandlerClassMap = new TIntObjectHashMap<>();
    private static TIntObjectHashMap<Set<Class<?>>> eventListenerHandlerClassMap = new TIntObjectHashMap<>();
    private static TIntObjectHashMap<Set<Class<?>>> eventSynListenerHandlerClassMap = new TIntObjectHashMap<>();
    private static TIntObjectHashMap<Class<?>> netEventListenerHandlerClassMap = new TIntObjectHashMap<>();
    private static Map<Class<?>, List<Method>> updatableClassMap = new HashMap<>();
    private static Map<Class<?>, List<Method>> monitorClassMap = new HashMap<>();
    private static Map<String,Method> remoteMethodIndex = new HashMap<>();
    private static Map<Method,Method> overrideMethod = new HashMap<>(); // 被重写的方法：key重写之后的-value重写之前的


    /**
     * BeanHelp获取该Map对Service进行实例化，
     * 这样确保了同一个Service只实例化一次
     */
    private static Map<Class<?>, Class<?>> serviceClassMap = new HashMap<>();
    private final static Map<Class<?>, Class<?>> serviceOriginClass = new HashMap<>();

    // 各个service的初始化方法和销毁方法,threeMap默认是根据键之排序的
    private static Map<Integer, Map<Class<?>, Method>> initMethodMap = new TreeMap<>();
    private static Map<Integer, Map<Class<?>, Method>> destroyMethodMap = new TreeMap<>();
    private static Map<String, Method> gmMethod = new HashMap<>();
    private static Map<String, Method> statisticsMethods = new HashMap<>();
    private static Map<String,Class> serviceNameClassMap = new HashMap<>();

    static {
        try {
            Map<Class<?>, List<Method>> requestMap = new HashMap<>();
            Map<Class<?>, List<Method>> eventListenerMap = new HashMap<>();
            Map<Class<?>, List<Method>> netEventListenerMap = new HashMap<>();
            Map<Class<?>, List<Method>> updatableMap = new HashMap<>();
            Map<Class<?>, List<Method>> remoteMap = new HashMap<>();
            Map<Class<?>, List<Method>> monitorMap = new HashMap<>();

            List<Class<?>> serviceClasses = ClassHelper.getClassListByAnnotation(Service.class,
                    Server.getEngineConfigure().getAllPackets());
            for (Class<?> serviceClass : serviceClasses) {
                serviceNameClassMap.put(serviceClass.getName(),serviceClass);
                Service service = serviceClass.getAnnotation(Service.class);
                String init = service.init();
                // 判断是否是初始化方法和销毁方法
                if(StringUtils.isEmpty(init)){
                    log.info("service "+serviceClass.getName()+" has no init method");
                }
                int initPriority = service.initPriority();
                String destroy = service.destroy();
                int destroyPriority = service.destroyPriority();
                Method[] methods = serviceClass.getMethods();
                for (Method method : methods) {

                    // @Tx 参数检查.
                    if (method.isAnnotationPresent(Tx.class)) {
                        for (Class<?> p : method.getParameterTypes()) {
                            if (p.isAnnotationPresent(DBEntity.class)) {
                                throw new MMException(String.format("@Tx事物接口不允许用DBEntity实例当参数 Service: %s method: %s paraType: %s", serviceClass.getSimpleName(), method.getName(), p.getSimpleName()));
                            }
                        }
                    }

                    // 判断是否存在Request
                    if (method.isAnnotationPresent(Request.class)) {
                        addMethodToMap(requestMap, serviceClass, method);
                    }

                    if (method.isAnnotationPresent(EventListener.class)) {
                        addMethodToMap(eventListenerMap, serviceClass, method);
                    }

                    if (method.isAnnotationPresent(NetEventListener.class)) {
                        addMethodToMap(netEventListenerMap, serviceClass, method);
                    }

                    if (method.isAnnotationPresent(Updatable.class)) {
                        addMethodToMap(updatableMap, serviceClass, method);
                    }

                    if (method.isAnnotationPresent(Remotable.class)) {
                        addMethodToMap(remoteMap, serviceClass, method);
                    }

                    if (method.isAnnotationPresent(Monitor.class)) {
                        addMethodToMap(monitorMap, serviceClass, method);
                    }

                    Gm gm = method.getAnnotation(Gm.class);
                    if (gm != null) {
                        Method old = gmMethod.put(gm.id(), method);
                        if (old != null) {
                            throw new MMException("gm id duplicate,id=" + gm.id() + " at " + method.getDeclaringClass().getName() + "." + method.getName()
                                    + " and " + old.getDeclaringClass().getName() + "." + old.getName());
                        }
                    }

                    Statistics statistics = method.getAnnotation(Statistics.class);
                    if (statistics != null) {
                        //检查方法的合法性
                        Class[] parameterTypes = method.getParameterTypes();
                        // 检查参数
                        if (parameterTypes.length > 0) {
                            throw new MMException("Method " + method.getName() + " Parameter Error");
                        }
                        // 检查返回值
                        if (method.getReturnType() != StatisticsData.class) {
                            throw new IllegalStateException("Method " + method.getName() + " ReturnType Error");
                        }
                        statisticsMethods.put(statistics.id(), method);
                    }
                    if(method.getName().equals(init)){
                        initMethodMap.computeIfAbsent(initPriority,(k)->new HashMap<>()).put(serviceClass, method);
                    }
                    if(method.getName().equals(destroy)){
                        destroyMethodMap.computeIfAbsent(destroyPriority,(k)->new HashMap<>()).put(serviceClass, method);
                    }
                }
            }

            ClassPool pool = ClassPool.getDefault();

            /**
             * 获取每个Service被引用的情况，并生成新的Class
             *
             * 这一步生成需要修改BeanHelper中的对应的类和实例化对象
             */
            for (Class<?> serviceClass : serviceClasses) {
                // 对于request，用opcode导航
                List<Integer> opcodeList = null;
                List<Integer> eventList = null;
                List<Integer> eventSynList = null;
                List<Integer> netEventList = null;

                Class<?> newServiceClass = serviceClass;

                Service service = serviceClass.getAnnotation(Service.class);

                // 远程方法处理
                if(!Server.getEngineConfigure().getBoolean("server.is.test", false)) {
                    newServiceClass = genRemoteMethod(pool, serviceClass, newServiceClass);
                }

                if (requestMap.containsKey(serviceClass)) {
                    newServiceClass = generateRequestHandlerClass(newServiceClass, serviceClass);
                    opcodeList = new ArrayList<>();
                    List<Method> methodList = requestMap.get(serviceClass);
                    for (Method method : methodList) {
                        Request request = method.getAnnotation(Request.class);
                        opcodeList.add(request.opcode());
                    }
                }
                if (eventListenerMap.containsKey(serviceClass)) {
                    newServiceClass = generateEventListenerHandlerClass(newServiceClass, serviceClass);
                    List<Method> methodList = eventListenerMap.get(serviceClass);
                    for (Method method : methodList) {
                        EventListener request = method.getAnnotation(EventListener.class);
                        if(request.sync()){
                            if(eventSynList == null){
                                eventSynList = new ArrayList<>();
                            }
                            eventSynList.add(request.event());
                        }else {
                            if(eventList == null){
                                eventList = new ArrayList<>();
                            }
                            eventList.add(request.event());
                        }
                    }
                }
                if (netEventListenerMap.containsKey(serviceClass)) {
                    newServiceClass = generateNetEventListenerHandlerClass(newServiceClass, serviceClass);
                    netEventList = new ArrayList<>();
                    List<Method> methodList = netEventListenerMap.get((serviceClass));
                    for (Method method : methodList) {
                        NetEventListener netEventListener = method.getAnnotation(NetEventListener.class);
                        netEventList.add(netEventListener.netEvent());
                    }
                }
                if (updatableMap.containsKey(serviceClass)) {
                    newServiceClass = generateUpdatableHandlerClass(newServiceClass, serviceClass);
                }
                if (monitorMap.containsKey(serviceClass)) {
                    newServiceClass = generateMonitorHandlerClass(newServiceClass, serviceClass);
                }

                serviceClassMap.put(serviceClass, newServiceClass);
                serviceOriginClass.put(newServiceClass, serviceClass);
                // request
                if (opcodeList != null) {
                    for (int opcode : opcodeList) {
                        requestHandlerClassMap.put(opcode, serviceClass);
                    }
                }
                // event
                if (eventList != null) {
                    // 一个event可能对应多个类
                    for (int event : eventList) {
                        if (eventListenerHandlerClassMap.containsKey(event)) {
                            Set<Class<?>> classes = eventListenerHandlerClassMap.get(event);
                            classes.add(serviceClass);
                        } else {
                            Set<Class<?>> classes = new HashSet<>();
                            classes.add(serviceClass);
                            eventListenerHandlerClassMap.put(event, classes);
                        }
                    }
                }
                if (eventSynList != null) {
                    // 一个event可能对应多个类
                    for (int event : eventSynList) {
                        if (eventSynListenerHandlerClassMap.containsKey(event)) {
                            Set<Class<?>> classes = eventSynListenerHandlerClassMap.get(event);
                            classes.add(serviceClass);
                        } else {
                            Set<Class<?>> classes = new HashSet<>();
                            classes.add(serviceClass);
                            eventSynListenerHandlerClassMap.put(event, classes);
                        }
                    }
                }
                // netEvent
                if (netEventList != null) {
                    // 一个netevent可能对应多个类
                    for (int netEvent : netEventList) {
                        netEventListenerHandlerClassMap.put(netEvent, serviceClass);
                    }
                }
                // update
                if (updatableMap.containsKey(serviceClass)) {
                    updatableClassMap.put(serviceClass, updatableMap.get(serviceClass));
                }
                // monitor
                if (monitorMap.containsKey(serviceClass)) {
                    monitorClassMap.put(serviceClass, monitorMap.get(serviceClass));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("ServiceHelper init Exception",e);
        }
    }

    /**
     * 处理非单一服务的 Remote Service 方法
     *
     * @param pool
     * @param serviceClass
     * @param newServiceClass
     * @return
     * @throws Exception
     */
    private static Class<?> genRemoteMethod(ClassPool pool, Class<?> serviceClass, Class<?> newServiceClass) throws Exception {
        Map<Method, Remotable> remoteMethods = Maps.newHashMap();

        // 只处理 当前类定义的 public 的方法
        for (Method method : serviceClass.getMethods()) {
            Remotable remote = method.getAnnotation(Remotable.class);
            if(remote == null) {
                continue;
            }
            if(method.getReturnType().isPrimitive() && method.getReturnType() != Void.TYPE) {
                throw new MMException(String.format("Remote接口返回值类型不允许为基本类型. Service: %s method: %s returnType: %s", serviceClass.getSimpleName(), method.getName(), method.getReturnType()));
            }
            remoteMethods.put(method, remote);
            remoteMethodIndex.put(ClassUtil.getMethodSignature(method),method);
        }

        if(remoteMethods.isEmpty()) {
            return newServiceClass;
        }

        CtClass ctClazz = pool.get(serviceClass.getName());
        CtClass proxyClazz = pool.makeClass(ctClazz.getName() + "$Proxy", ctClazz);

        List<Method> oldMethodList = new ArrayList<>();
        // 改写remote方法, 使用远程调用
        for(Map.Entry<Method, Remotable> en:remoteMethods.entrySet()) {
            Remotable remote = en.getValue();
            Method oldMethod = en.getKey();
            oldMethodList.add(oldMethod);

            CtMethod ctMethod = ctClazz.getMethod(oldMethod.getName(), packDescreptor(oldMethod));
            CtMethod proxyMethod = CtNewMethod.copy(ctMethod, proxyClazz, null);
            for(Object attr: ctMethod.getMethodInfo().getAttributes()){
                if(attr.getClass().isAssignableFrom(AnnotationsAttribute.class)){
                    AnnotationsAttribute attribute = (AnnotationsAttribute)attr;
                    proxyMethod.getMethodInfo().addAttribute(attribute);
                }
            }

            /**
             * 实际的执行逻辑
             *
             * 1: 通过 routeType 计算得到 serverId
             * 2: 根据 serverId 调用本地或远程 Service 的相应方法.
             */
            StringBuilder body = new StringBuilder();
            {
                String argsString = genArgsString(oldMethod);

                body.append("{\n");
                body.append(String.format("\t%s route = %s.%s;\n", IRoute.class.getName(), RouteType.class.getName(), remote.route()));
                if(remote.routeArgIndex()>0 && oldMethod.getParameterTypes().length>=remote.routeArgIndex()) {
                    Class<?> paramClass = oldMethod.getParameterTypes()[remote.routeArgIndex()-1];
                    String param = "$" + remote.routeArgIndex();
                    if(Object.class.isAssignableFrom(paramClass)){

                    }else{
                        if(paramClass == int.class){
//                            for(int i=0;i<oldMethod.getParameterTypes().length;i++){
//                                System.out.println(oldMethod.getParameterTypes()[i]);
//                            }
//                            log.info("-------------------"+en.getKey()+","+en.getValue()+",");
                            param = "new Integer($" + remote.routeArgIndex() + ")";
                        }else if(paramClass == long.class){
                            param = "new Long($" + remote.routeArgIndex() + ")";
                        }else if(paramClass == boolean.class){
                            param = "new Boolean($" + remote.routeArgIndex() + ")";
                        }else if(paramClass == short.class){
                            param = "new Short($" + remote.routeArgIndex() + ")";
                        }else if(paramClass == byte.class){
                            param = "new Byte($" + remote.routeArgIndex() + ")";
                        }else if(paramClass == float.class){
                            param = "new Float($" + remote.routeArgIndex() + ")";
                        }else if(paramClass == double.class){
                            param = "new Double($" + remote.routeArgIndex() + ")";
                        }else if(paramClass == char.class){
                            param = "new Character($" + remote.routeArgIndex() + ")";
                        }

                    }
//                    if (remote.route().getFirstArgType().isAssignableFrom(int.class)) { // 这个地方改成判断第一个参数类型，如果是基本类型改成装箱
//                        param = "new Integer($" + remote.routeArgIndex() + ")";
//                    }
                    body.append(String.format("\tint serverId = route.getServerId(" + param + ");\n"));
                }else{
                    // 有些远程调用不需要参数
                    body.append(String.format("\tint serverId = route.getServerId(null);\n"));
                }
                // body.append(String.format("\tif(serverId == 0 || serverId == %s.getServerId().intValue()) {\n", Server.class.getName()));
                body.append(String.format("\tif((%s.getEngineConfigure().getBoolean(\"server.is.test\", false)) || serverId == %s.getServerId().intValue() ) {\n", Server.class.getName(), Server.class.getName()));


                if (oldMethod.getReturnType() != Void.TYPE) {
                    body.append("\t\treturn ");
                }
                body.append(String.format(" super.%s(%s);\n", oldMethod.getName(), argsString));
                body.append(String.format("\t}else{\n"));
                String remoteSerName = RemoteCallService.class.getName();
                body.append(String.format("\t\t%s remoteService = (%s)%s.getServiceBean(%s.class);\n", remoteSerName, remoteSerName, BeanHelper.class.getName(), remoteSerName));
                if (oldMethod.getReturnType() != Void.TYPE) {
//                    body.append(String.format("\t\tObject object = remoteService.remoteCallSyn(serverId, %s.class,\"%s\",$args,%s);\n", serviceClass.getName(), ctMethod.getName(),RemoteExceptionHandler.class.getName()+"."+remote.remoteExceptionHandler().name()));
                    body.append(String.format("\t\tObject object = remoteService.remoteCallSyn(serverId, %s.class,\"%s\",\"%s\",$args,%s);\n", serviceClass.getName(), ctMethod.getName(),ClassUtil.getMethodSignature(oldMethod),RemoteExceptionHandler.class.getName()+"."+remote.remoteExceptionHandler().name()));

                    //body.append(String.format("\t\treturn (%s)object;", oldMethod.getReturnType().getName()));
                    body.append("\t\treturn "+ parseObjectTypeStrToBaseTypeStr(oldMethod.getReturnType().getName(),"object"));

                } else {
//                    body.append(String.format("remoteService.remoteCallSyn(serverId, %s.class,\"%s\",$args,%s);\n", serviceClass.getName(), ctMethod.getName(),RemoteExceptionHandler.class.getName()+"."+remote.remoteExceptionHandler().name()));
                    body.append(String.format("remoteService.remoteCallSyn(serverId, %s.class,\"%s\",\"%s\",$args,%s);\n", serviceClass.getName(), ctMethod.getName(),ClassUtil.getMethodSignature(oldMethod),RemoteExceptionHandler.class.getName()+"."+remote.remoteExceptionHandler().name()));
                }
                body.append(String.format("\n\t}\n"));
                body.append("}");
            }
//            log.info("+++++++++++++++Remote Method++++++++++++++++++\n"+ctMethod.getLongName()+"\n"+body.toString()+"\n+++++++++++++++++++++++++++++++++++++++");
            proxyMethod.setBody(body.toString());
            proxyClazz.addMethod(proxyMethod);
        }
        newServiceClass = proxyClazz.toClass();

        for(Method oldMethod : oldMethodList){
            Method newMethod = newServiceClass.getMethod(oldMethod.getName(),oldMethod.getParameterTypes());
            overrideMethod.put(newMethod,oldMethod);
        }

        return newServiceClass;
    }


    public static Map<String,Method> getRemoteMethodIndex(){
        return remoteMethodIndex;
    }

    public static Map<Method, Method> getOverrideMethod() {
        return overrideMethod;
    }

    private static String genArgsString(Method oldMethod) {
        StringBuilder args = new StringBuilder("");
        for(int i=1; i <= oldMethod.getParameterCount(); i++) {
            args.append("$"+i).append(",");
        }
        if(args.length() > 0) {
            args.deleteCharAt(args.length() - 1);
        }
        return args.toString();
    }

    private static String packDescreptor(Method current) {
        Class<?>[] parameterTypes = current.getParameterTypes();
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (Class<?> pt : parameterTypes) {
            sb.append(packDescreptor(pt));
        }
        sb.append(")");
        Class<?> returnType = current.getReturnType();
        sb.append(packDescreptor(returnType));
        return sb.toString();
    }

    //boolean z, char c, byte b, short s, int i, long j, float f, double d
    private static String packDescreptor(Class<?> pt) {
        if(pt.equals(boolean.class)) {
            return "Z";
        } else if(pt.equals(char.class)) {
            return "C";
        } else if(pt.equals(byte.class)) {
            return "B";
        } else if(pt.equals(short.class)) {
            return "S";
        } else if(pt.equals(int.class)) {
            return "I";
        } else if(pt.equals(long.class)) {
            return "J";
        } else if(pt.equals(float.class)) {
            return "F";
        } else if(pt.equals(double.class)) {
            return "D";
        } else if(pt.equals(void.class)) {
            return "V";
        } else {
            return "L" + pt.getName().replaceAll("\\.", "/") + ";";
        }
    }

    public static String parseBaseTypeStrToObjectTypeStr(String typeStr, String paramStr) {
        if (typeStr.equals("byte")) {
            return "new Byte(" + paramStr + ")";
        } else if (typeStr.equals("short")) {
            return "new Short(" + paramStr + ")";
        } else if (typeStr.equals("long")) {
            return "new Long(" + paramStr + ")";
        } else if (typeStr.equals("int")) {
            return "new Integer(" + paramStr + ")";
        } else if (typeStr.equals("float")) {
            return "new Float(" + paramStr + ")";
        } else if (typeStr.equals("double")) {
            return "new Double(" + paramStr + ")";
        } else if (typeStr.equals("char")) {
            return "new Character(" + paramStr + ")";
        } else if (typeStr.equals("boolean")) {
            return "new Boolean(" + paramStr + ")";
        }
        return paramStr;
    }

    public static String parseObjectTypeStrToBaseTypeStr(String typeStr,String objectStr) {
        if (typeStr.equals("byte")) {
            return objectStr+"==null?0:((Byte)"+objectStr+").byteValue()";
        } else if (typeStr.equals("short")) {
            return objectStr+"==null?0:((Short)"+objectStr+").shortValue()";//"Short";
        } else if (typeStr.equals("long")) {
            return objectStr+"==null?0:((Long)"+objectStr+").longValue()";//"Long";
        } else if (typeStr.equals("int")) {
            return objectStr+"==null?0:((Integer)"+objectStr+").intValue()";//"Integer";
        } else if (typeStr.equals("float")) {
            return objectStr+"==null?0:((Float)"+objectStr+").floatValue()";//"Float";
        } else if (typeStr.equals("double")) {
            return objectStr+"==null?0:((Double)"+objectStr+").doubleValue()";//"Double";
        } else if (typeStr.equals("char")) {
            return objectStr+"==null?0:((Character)"+objectStr+").charValue()";//"Character";
        } else if (typeStr.equals("boolean")) {
            return objectStr+"==null?false:((Boolean)"+objectStr+").booleanValue()";//"Boolean";
        }
        return "(" + typeStr + ")"+objectStr+"";
    }

    public static String arrayToStr(Class cls){
        if(!cls.isArray()){
            return cls.getName();
        }
        StringBuilder sb = new StringBuilder();
        while (cls.isArray()){
            sb.append("[]");
            cls = cls.getComponentType();
        }
        sb.insert(0,cls.getName());
        return sb.toString();
    }

    //get set
    public static Map<Class, List<Method>> getMethodsByAnnotation(Class<? extends Annotation> cls) {
        List<Class<?>> serviceClasses = ClassHelper.getClassListByAnnotation(Service.class,
                Server.getEngineConfigure().getAllPackets());
        Map<Class, List<Method>> result = new HashMap<>();
        for (Class<?> serviceClass : serviceClasses) {
            Service service = serviceClass.getAnnotation(Service.class);
            Method[] methods = serviceClass.getMethods();
            for (Method method : methods) {
                Annotation annotation = method.getAnnotation(cls);
                if (annotation != null) {
                    List<Method> list = result.get(serviceClass);
                    if (list == null) {
                        list = new ArrayList<>();
                        result.put(serviceClass, list);
                    }
                    list.add(method);
                }
            }
        }
        return result;
    }

    public static TIntObjectHashMap<Class<?>> getRequestHandlerMap() {
        return requestHandlerClassMap;
    }

    public static TIntObjectHashMap<Set<Class<?>>> getEventListenerHandlerClassMap() {
        return eventListenerHandlerClassMap;
    }
    public static TIntObjectHashMap<Set<Class<?>>> getEventSynListenerHandlerClassMap() {
        return eventSynListenerHandlerClassMap;
    }

    public static TIntObjectHashMap<Class<?>> getNetEventListenerHandlerClassMap() {
        return netEventListenerHandlerClassMap;
    }

    public static Map<Class<?>, List<Method>> getUpdatableClassMap() {
        return updatableClassMap;
    }

    public static Map<Class<?>, List<Method>> getMonitorClassMap() {
        return monitorClassMap;
    }

    public static Map<Class<?>, Class<?>> getServiceClassMap() {
        return serviceClassMap;
    }

    public static Class<?> getOriginServiceClass(Class<?> cls) {
        return serviceOriginClass.get(cls);
    }

    public static Map<Integer, Map<Class<?>, Method>> getInitMethodMap() {
        return initMethodMap;
    }

    public static Map<Integer, Map<Class<?>, Method>> getDestroyMethodMap() {
        return destroyMethodMap;
    }

    public static Map<String, Method> getGmMethod() {
        return gmMethod;
    }

    public static Map<String, Method> getStatisticsMethods() {
        return statisticsMethods;
    }

    //
    private static void addMethodToMap(Map<Class<?>, List<Method>> map, Class<?> cls, Method method) {
        if (map.containsKey(cls)) {
            List<Method> list = map.get(cls);
            list.add(method);
        } else {
            List<Method> list = new ArrayList<>();
            list.add(method);
            map.put(cls, list);
        }
    }



    // 生成request的处理类
    private static Class generateRequestHandlerClass(Class clazz, Class<?> oriClass) throws Exception {
        Map<Integer, String> opMethods = new TreeMap<Integer, String>();
        Map<Integer, String> paramsType = new TreeMap<Integer, String>(); // 第一个参数类型的字符串
        Method[] methods = oriClass.getDeclaredMethods();
        for (Method method : methods) { //遍历所有方法，将其中标注了是包处理方法的方法名加入到opMethods中
            if (method.isAnnotationPresent(Request.class)) {
                Request op = method.getAnnotation(Request.class);
                Class[] parameterTypes = method.getParameterTypes();
                //检查方法的合法性
                // 检查参数
                if (parameterTypes.length != 2) {
                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                }
                // -----------------add
                paramsType.put(op.opcode(),parameterTypes[0].toString().replace("class ",""));
//                System.out.println(parameterTypes[0].toString());
                if(parameterTypes[1] != Session.class){
                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                }
                opMethods.put(op.opcode(), method.getName());
            }
        }
        if (opMethods.size() > 0 ) {
            ClassPool pool = ClassPool.getDefault();

            CtClass oldClass = pool.get(clazz.getName());
//			log.info("oldClass: " + oldClass);
            CtClass ct = pool.makeClass(oldClass.getName() + "$Proxy", oldClass); //这里需要生成一个新类，并且继承自原来的类
            CtClass superCt = pool.get(RequestHandler.class.getName());  //需要实现RequestHandler接口
            ct.addInterface(superCt);
            if (opMethods.size() >= 0) {  // todo 为啥这个地方如果不进去，不创建handle方法，却不报错？？因为可以构建成功，且在调用的时候才检查，事实上可以让他有个空方法更保险
                //添加handler方法，在其中添上switch...case段
                StringBuilder sb = new StringBuilder("public Object handle(" +
                        "int opcode,Object clientData,com.peony.core.data.entity.session.Session session) throws Exception{");
                sb.append("Object rePacket=null;");
                if(opMethods.size()>0) {
                    sb.append("short opCode = opcode;");//$1.getOpcode();");
                    sb.append("switch (opCode) {");
                    Iterator<Map.Entry<Integer, String>> ite = opMethods.entrySet().iterator();
                    while (ite.hasNext()) {
                        Map.Entry<Integer, String> entry = ite.next();
                        sb.append("case ").append(entry.getKey()).append(":");
                        sb.append("rePacket=").append(entry.getValue()).append("(("+paramsType.get(entry.getKey())+")$2,$3);"); //注意，这里所有的方法都必须是protected或者是public的，否则此部生成会出错
                        sb.append("break;");
                        //opcodes.add(entry.getKey());
                    }
                    sb.append("}");
                }
                sb.append("return rePacket;");
                sb.append("}");
                CtMethod method = CtMethod.make(sb.toString(), ct);
                ct.addMethod(method);
            }
//            System.err.println(clazz);

            return ct.toClass();
        } else {
            return clazz;
        }
    }

    // 生成event的处理类
    private static Class generateEventListenerHandlerClass(Class clazz, Class<?> oriClass) throws Exception {
        Map<Integer, List<String>> opMethods = new TreeMap<Integer, List<String>>();
        Map<Integer, List<String>> opSynMethods = new TreeMap<Integer, List<String>>(); // 同步事件
        Method[] methods = oriClass.getDeclaredMethods();
        Map<Integer, Class> paramsType = new TreeMap<Integer, Class>(); // 第一个参数类型的字符串
//        Method[] methods = clazz.getMethods();
        for (Method method : methods) { //遍历所有方法，将其中标注了是包处理方法的方法名加入到opMethods中
            if (method.isAnnotationPresent(EventListener.class)) {
                EventListener op = method.getAnnotation(EventListener.class);
                Class[] parameterTypes = method.getParameterTypes();
                //检查方法的合法性
                // 检查参数
                if (parameterTypes.length != 1) {
                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                }
//                paramsType.put(op.event(),parameterTypes[0].toString().replace("class ",""));
                paramsType.put(op.event(),parameterTypes[0]);
//                if (parameterTypes[0] != EventData.class) {
//                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
//                }
                // 一个类中可能存在多个对该事件的监听
                if(op.sync()){
                    if (opSynMethods.containsKey(op.event())) {
                        opSynMethods.get(op.event()).add(method.getName());
                    } else {
                        List<String> methodNames = new ArrayList<>();
                        methodNames.add(method.getName());
                        opSynMethods.put(op.event(), methodNames);
                    }
                }else {
                    if (opMethods.containsKey(op.event())) {
                        opMethods.get(op.event()).add(method.getName());
                    } else {
                        List<String> methodNames = new ArrayList<>();
                        methodNames.add(method.getName());
                        opMethods.put(op.event(), methodNames);
                    }
                }
            }
        }
        if (opMethods.size() > 0 || opSynMethods.size() > 0) {
            ClassPool pool = ClassPool.getDefault();
            CtClass oldClass = pool.get(clazz.getName());
            CtClass ct = pool.makeClass(oldClass.getName() + "$Proxy", oldClass); //这里需要生成一个新类，并且继承自原来的类
            CtClass superCt = pool.get(EventListenerHandler.class.getName());  //需要实现RequestHandler接口
            ct.addInterface(superCt);
            if(opMethods.size() > 0) {
                //添加handler方法，在其中添上switch...case段
                appendEventHandler(opMethods,ct,paramsType,false);
            }
            if(opSynMethods.size() > 0) {
                //添加handler方法，在其中添上switch...case段
                appendEventHandler(opSynMethods,ct,paramsType,true);
            }
            return ct.toClass();
        } else {
            return clazz;
        }
    }
    private static void appendEventHandler(Map<Integer, List<String>> opMethods,CtClass ct,Map<Integer, Class> paramsType,boolean sync) throws Exception{
        //添加handler方法，在其中添上switch...case段
        StringBuilder sb = new StringBuilder("public void "+(sync?"handleSyn":"handle")+"(" +
                "int event,Object data) throws Exception{");
//        sb.append("int event = $1.getEvent();");//$1.getOpcode();");
        sb.append("switch (event) {");
        Iterator<Map.Entry<Integer, List<String>>> ite = opMethods.entrySet().iterator();
        while (ite.hasNext()) {
            Map.Entry<Integer, List<String>> entry = ite.next();
            sb.append("case ").append(entry.getKey()).append(":");
            for (String meName : entry.getValue()) {
                Class typeCls  = paramsType.get(entry.getKey());
                if(typeCls.isPrimitive()){
                    sb.append("try{");
                    String parseStr = parsePrimitiveStringByTypeClass(typeCls);
                    sb.append(meName).append("("+parseStr+");");
                    sb.append("}catch(ClassCastException e){");
                    sb.append("org.slf4j.LoggerFactory.getLogger(this.getClass()).error(\"[Peony] Event param type cast error!listener="+typeCls+",firer={}\",$2.getClass().getName());");
                    sb.append("throw new com.peony.common.exception.MMException(\"[Peony] Event param type cast error!listener="+typeCls+",firer=\"+$2.getClass().getName(),e);");
                    sb.append("}");
                }else{
//                    System.out.println(typeCls.toString().replace("class ",""));
                    sb.append(meName).append("(("+typeCls.getName()+")$2);");
                }
                //parameterTypes[0].toString().replace("class ","")
//                sb.append(meName).append("(("+paramsType.get(entry.getKey())+")$2);"); //注意，这里所有的方法都必须是protected或者是public的，否则此部生成会出错
            }
            sb.append("break;");
            //opcodes.add(entry.getKey());
        }
        sb.append("}");
        sb.append("}");
//        System.out.println(sb);
        CtMethod method = CtMethod.make(sb.toString(), ct);
        ct.addMethod(method);
    }

    static String parsePrimitiveStringByTypeClass(Class typeCls){
        if(typeCls == int.class){
            return "((Integer)$2).intValue()";
        }else if(typeCls == long.class){
            return "((Long)$2).longValue()";
        }else if(typeCls == boolean.class){
            return "((Boolean)$2).booleanValue()";
        }else if(typeCls == float.class){
            return "((Float)$2).floatValue()";
        }else if(typeCls == double.class){
            return "((Double)$2).doubleValue()";
        }else if(typeCls == short.class){
            return "((Short)$2).shortValue()";
        }else if(typeCls == char.class){
            return "((Character)$2).charValue()";
        }else if(typeCls == byte.class){
            return "((Byte)$2).byteValue()";
        }
        throw new MMException("typeCls error!typeCls:"+typeCls);
    }

    private static Class generateNetEventListenerHandlerClass(Class clazz, Class<?> oriClass) throws Exception {
        Map<Integer, String> opMethods = new TreeMap<Integer, String>();
        Method[] methods = oriClass.getDeclaredMethods();

        for (Method method : methods) { //遍历所有方法，将其中标注了是包处理方法的方法名加入到opMethods中
            if (method.isAnnotationPresent(NetEventListener.class)) {
                NetEventListener op = method.getAnnotation(NetEventListener.class);
                Class[] parameterTypes = method.getParameterTypes();
                //检查方法的合法性
                // 检查参数
                if (parameterTypes.length != 1) {
                    throw new IllegalStateException("Method " + oriClass.getSimpleName() + "."+ method.getName() + " Parameter Error");
                }
                if (parameterTypes[0] != NetEventData.class) {
                    throw new IllegalStateException("Method " + oriClass.getSimpleName() + "."+ method.getName() + " Parameter Error");
                }
                // 检查返回值
                if (method.getReturnType() != NetEventData.class) {
                    throw new IllegalStateException("Method " + oriClass.getSimpleName() + "." + method.getName() + " ReturnType Error");
                }
                opMethods.put(op.netEvent(), method.getName());
            }
        }
        if (opMethods.size() > 0) {
            ClassPool pool = ClassPool.getDefault();
            CtClass oldClass = pool.get(clazz.getName());
            CtClass ct = pool.makeClass(oldClass.getName() + "$Proxy", oldClass); //这里需要生成一个新类，并且继承自原来的类
            CtClass superCt = pool.get(NetEventListenerHandler.class.getName());  //需要实现RequestHandler接口
            ct.addInterface(superCt);
            //添加handler方法，在其中添上switch...case段
            StringBuilder sb = new StringBuilder("public com.peony.core.control.netEvent.NetEventData handle(" +
                    "com.peony.core.control.netEvent.NetEventData netEventData) throws Exception{");
            sb.append("com.peony.core.control.netEvent.NetEventData rePacket=null;");
            sb.append("int event = $1.getNetEvent();");//$1.getOpcode();");
            sb.append("switch (event) {");
            Iterator<Map.Entry<Integer, String>> ite = opMethods.entrySet().iterator();
            while (ite.hasNext()) {
                Map.Entry<Integer, String> entry = ite.next();
                sb.append("case ").append(entry.getKey()).append(":");
                sb.append("rePacket=").append(entry.getValue()).append("($1);"); //注意，这里所有的方法都必须是protected或者是public的，否则此部生成会出错
                sb.append("break;");
            }
            sb.append("}");
            sb.append("return rePacket;");
            sb.append("}");
            CtMethod method = CtMethod.make(sb.toString(), ct);
            ct.addMethod(method);
            return ct.toClass();
        } else {
            return clazz;
        }
    }

    // 校验参数
    private static Class generateUpdatableHandlerClass(Class clazz, Class<?> oriClass) throws Exception {
        Method[] methods = oriClass.getDeclaredMethods();
        for (Method method : methods) { //遍历所有方法，将其中标注了是包处理方法的方法名加入到opMethods中
            if (method.isAnnotationPresent(Updatable.class)) {
                Updatable op = method.getAnnotation(Updatable.class);
                Class[] parameterTypes = method.getParameterTypes();
                //检查方法的合法性
                // 检查参数
                if (parameterTypes.length != 1) {
                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                }
                if (parameterTypes[0] != Integer.class && parameterTypes[0] != int.class) {
                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                }
                // 检查注解
                if (op.cycle() == -1 && op.cronExpression().length() == 0) {
                    throw new IllegalStateException("Method " + method.getName() + " annotation Error,cycle can't be default while update is asynchronous");
                }
            }
        }
        return clazz;
    }

    // 校验参数
    private static Class generateMonitorHandlerClass(Class clazz, Class<?> oriClass) throws Exception {
        Method[] methods = oriClass.getDeclaredMethods();
        for (Method method : methods) { //遍历所有方法，将其中标注了是包处理方法的方法名加入到opMethods中
            if (method.isAnnotationPresent(Monitor.class)) {
                Monitor op = method.getAnnotation(Monitor.class);
                Class[] parameterTypes = method.getParameterTypes();
                //检查方法的合法性
                // 检查参数
                if (parameterTypes.length != 0) {
                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                }
            }
        }
        return clazz;
    }

    public static Class getServiceClassByName(String name){
        return serviceNameClassMap.get(name);
    }
}
