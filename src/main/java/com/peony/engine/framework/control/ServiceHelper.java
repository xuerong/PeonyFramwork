package com.peony.engine.framework.control;

import com.google.api.client.util.Maps;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.control.netEvent.remote.RemoteCallService;
import com.peony.engine.framework.control.rpc.IRoute;
import com.peony.engine.framework.control.rpc.Remotable;
import com.peony.engine.framework.security.Monitor;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.control.rpc.RouteType;
import com.peony.engine.framework.control.statistics.Statistics;
import com.peony.engine.framework.control.statistics.StatisticsData;
import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.control.annotation.*;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.event.EventListenerHandler;
import com.peony.engine.framework.control.netEvent.NetEventListenerHandler;
import com.peony.engine.framework.control.request.RequestHandler;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.control.netEvent.NetEventData;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.ServerType;
import com.peony.engine.framework.tool.helper.ClassHelper;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import javassist.*;
import javassist.bytecode.MethodInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * Created by Administrator on 2015/11/18.
 * ServiceHelper
 * 在系统启动的时候，找到所有的Service并用对应的Handler封装，
 * 然后传递给对应的管理器,分别为：
 * Request-RequestHandler-RequestService
 * EventListener-EventListenerHandler-EventService
 * NetEventListener-NetEventListenerHandler-NetEventService
 * Updatable-
 */
public final class ServiceHelper {
    private static final Logger log = LoggerFactory.getLogger(ServiceHelper.class);

    private static TIntObjectHashMap<Class<?>> requestHandlerClassMap = new TIntObjectHashMap<>();
    private static TShortObjectHashMap<Set<Class<?>>> eventListenerHandlerClassMap = new TShortObjectHashMap<>();
    private static TShortObjectHashMap<Set<Class<?>>> eventSynListenerHandlerClassMap = new TShortObjectHashMap<>();
    private static TIntObjectHashMap<Class<?>> netEventListenerHandlerClassMap = new TIntObjectHashMap<>();
    private static Map<Class<?>, List<Method>> updatableClassMap = new HashMap<>();
    private static Map<Class<?>, List<Method>> monitorClassMap = new HashMap<>();

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

    static {
        try {
            Map<Class<?>, List<Method>> requestMap = new HashMap<>();
            Map<Class<?>, List<Method>> eventListenerMap = new HashMap<>();
            Map<Class<?>, List<Method>> netEventListenerMap = new HashMap<>();
            Map<Class<?>, List<Method>> updatableMap = new HashMap<>();
            Map<Class<?>, List<Method>> remoteMap = new HashMap<>();
            Map<Class<?>, List<Method>> monitorMap = new HashMap<>();

            List<Class<?>> serviceClasses = ClassHelper.getClassListByAnnotation(Service.class);
            for (Class<?> serviceClass : serviceClasses) {
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

                    //
                    if (!service.runOnEveryServer() && !ServerType.isMainServer()) {
                        continue;
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
                List<Short> opcodeList = null;
                List<Short> eventList = null;
                List<Short> eventSynList = null;
                List<Integer> netEventList = null;

                Class<?> newServiceClass = serviceClass;

                Service service = serviceClass.getAnnotation(Service.class);

                // 单一服的某些特定 Service 如排行榜等
                if (!service.runOnEveryServer() && !ServerType.isMainServer()) {
                    // 改写所有的public方法,使用远程调用
                    CtClass oldClass = pool.get(serviceClass.getName());
                    CtClass cls = pool.makeClass(oldClass.getName() + "$Proxy", oldClass);

                    for (CtMethod ctMethod : oldClass.getDeclaredMethods()) {
                        MethodInfo methodInfo = ctMethod.getMethodInfo();
                        if (methodInfo.getAccessFlags() == 1) { // public 的
                            // 重写该方法
                            StringBuilder sb = new StringBuilder(ctMethod.getReturnType().getName() + " " + ctMethod.getName() + "(");
                            CtClass[] paramClasses = ctMethod.getParameterTypes();
                            int i = 0;
                            StringBuilder paramsStr = new StringBuilder();
                            for (CtClass paramClass : paramClasses) {
                                if (i > 0) {
                                    sb.append(",");
                                    paramsStr.append(",");
                                }
                                sb.append(paramClass.getName() + " p" + i);
                                // 这个地方需要进行一步强制转换,基本类型不能编译成Object类型
                                paramsStr.append(praseBaseTypeStrToObjectTypeStr(paramClass.getName(), "p" + i));
                                i++;
                            }
                            sb.append(") {");
                            String paramStr = "null";
                            if (i > 0) {
                                sb.append("Object[] param = new Object[]{" + paramsStr + "};");
                                paramStr = "param";
                            }
                            // --------------这个地方要进行强制转换
                            sb.append("com.peony.engine.framework.control.netEvent.remote.RemoteCallService remoteCallService = (com.peony.engine.framework.control.netEvent.remote.RemoteCallService)com.peony.engine.framework.tool.helper.BeanHelper.getServiceBean(com.peony.engine.framework.control.netEvent.remote.RemoteCallService.class);");
                            String invokeStr = "remoteCallService.remoteCallMainServerSyn(" + serviceClass.getName() + ".class,\"" + ctMethod.getName() + "\"," + paramStr + ");";
                            if (ctMethod.getReturnType().getName().toLowerCase().equals("void")) {
                                sb.append(invokeStr);
                            } else {
                                sb.append("Object object = " + invokeStr);
                                sb.append("return " + parseBaseTypeStrToObjectTypeStr(ctMethod.getReturnType().getName()));
                            }
                            sb.append("}");
                            log.info("==============================================\n"+sb.toString());
                            CtMethod method = CtMethod.make(sb.toString(), cls);
                            cls.addMethod(method);
                        }
                    }
                    newServiceClass = cls.toClass();

                } else {
                    // 远程方法处理
                    if(!Server.getEngineConfigure().getBoolean("server.is.test", false)) {
                        newServiceClass = genRemoteMethod(pool, serviceClass, newServiceClass);
                    }
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
                    for (short opcode : opcodeList) {
                        requestHandlerClassMap.put(opcode, serviceClass);
                    }
                }
                // event
                if (eventList != null) {
                    // 一个event可能对应多个类
                    for (short event : eventList) {
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
                    for (short event : eventSynList) {
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
            throw new RuntimeException("ServiceHelper init Exception");
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
        }

        if(remoteMethods.isEmpty()) {
            return newServiceClass;
        }

        CtClass ctClazz = pool.get(serviceClass.getName());
        CtClass proxyClazz = pool.makeClass(ctClazz.getName() + "$Proxy", ctClazz);

        // 改写remote方法, 使用远程调用
        for(Map.Entry<Method, Remotable> en:remoteMethods.entrySet()) {
            Remotable remote = en.getValue();
            Method oldMethod = en.getKey();

            CtMethod ctMethod = ctClazz.getMethod(oldMethod.getName(), packDescreptor(oldMethod));
            CtMethod proxyMethod = CtNewMethod.copy(ctMethod, proxyClazz, null);

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
                String param = "$"+remote.routeArgIndex();
                if(remote.route().getFirstArgType().isAssignableFrom(int.class)){
                    param = "new Integer($"+remote.routeArgIndex()+")";
                }
                body.append(String.format("\tint serverId = route.getServerId("+param+");\n"));
                //praseBaseTypeStrToObjectTypeStr
                // test
                // body.append(String.format("\tif(serverId == 0 || serverId == %s.getServerId().intValue()) {\n", Server.class.getName()));
                body.append(String.format("\tif((%s.getEngineConfigure().getBoolean(\"server.is.test\", false)) || serverId == %s.getServerId().intValue()) {\n", Server.class.getName(), Server.class.getName()));


                if (oldMethod.getReturnType() != Void.TYPE) {
                    body.append("\t\treturn ");
                }
                body.append(String.format(" super.%s(%s);\n", oldMethod.getName(), argsString));
                body.append(String.format("\t}else{\n"));
                String remoteSerName = RemoteCallService.class.getName();
                body.append(String.format("\t\t%s remoteService = (%s)%s.getServiceBean(%s.class);\n", remoteSerName, remoteSerName, BeanHelper.class.getName(), remoteSerName));
                if (oldMethod.getReturnType() != Void.TYPE) {
                    body.append(String.format("\t\tObject object = remoteService.remoteCallSyn(serverId, %s.class,\"%s\",$args);\n", serviceClass.getName(), ctMethod.getName()));
                    //body.append(String.format("\t\treturn (%s)object;", oldMethod.getReturnType().getName()));
                    body.append("\t\treturn "+parseBaseTypeStrToObjectTypeStr(oldMethod.getReturnType().getName())+";");
                } else {
                    body.append(String.format("remoteService.remoteCallSyn(serverId, %s.class,\"%s\",$args);\n", serviceClass.getName(), ctMethod.getName()));
                }
                body.append(String.format("\n\t}\n"));
                body.append("}");
            }
//            log.info("+++++++++++++++Remote Method++++++++++++++++++\n"+ctMethod.getLongName()+"\n"+body.toString()+"\n+++++++++++++++++++++++++++++++++++++++");
            proxyMethod.setBody(body.toString());
            proxyClazz.addMethod(proxyMethod);
        }
        newServiceClass = proxyClazz.toClass();
        return newServiceClass;
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

    private static String praseBaseTypeStrToObjectTypeStr(String typeStr, String paramStr) {
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

    private static String parseBaseTypeStrToObjectTypeStr(String typeStr) {
        if (typeStr.equals("byte")) {
            return "((Byte)object).byteValue();";
        } else if (typeStr.equals("short")) {
            return "((Short)object).shortValue();";//"Short";
        } else if (typeStr.equals("long")) {
            return "((Long)object).longValue();";//"Long";
        } else if (typeStr.equals("int")) {
            return "((Integer)object).intValue();";//"Integer";
        } else if (typeStr.equals("float")) {
            return "((Float)object).floatValue();";//"Float";
        } else if (typeStr.equals("double")) {
            return "((Double)object).doubleValue();";//"Double";
        } else if (typeStr.equals("char")) {
            return "((Character)object).charValue();";//"Character";
        } else if (typeStr.equals("boolean")) {
            return "((Boolean)object).booleanValue();";//"Boolean";
        }
        return "(" + typeStr + ")object";
    }

    //get set
    public static Map<Class, List<Method>> getMethodsByAnnotation(Class<? extends Annotation> cls) {
        List<Class<?>> serviceClasses = ClassHelper.getClassListByAnnotation(Service.class);
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

    public static TShortObjectHashMap<Set<Class<?>>> getEventListenerHandlerClassMap() {
        return eventListenerHandlerClassMap;
    }
    public static TShortObjectHashMap<Set<Class<?>>> getEventSynListenerHandlerClassMap() {
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
        Map<Short, Method> opMethods = new TreeMap<>();
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
                if (method.getReturnType() != Void.class) {
                    if (parameterTypes[1] != Session.class) {
                        throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                    }
                    opMethods.put(op.opcode(), method);
                } else {
                    throw new IllegalStateException("Method " + method.getName() + " ReturnType Error");
                }
                // -----------------add end
//                if(parameterTypes[0] != Object.class || parameterTypes[1] != Session.class){
//                    throw new IllegalStateException("Method "+method.getName()+" Parameter Error");
//                }
//                // 检查返回值
//                if(method.getReturnType()!=RetPacket.class){
//                    throw new IllegalStateException("Method "+method.getName()+" ReturnType Error");
//                }
//                opMethods.put(op.opcode(), method.getName());
            }
        }
        if (opMethods.size() > 0) {
            ClassPool pool = ClassPool.getDefault();

            CtClass oldClass = pool.get(clazz.getName());
//			log.info("oldClass: " + oldClass);
            CtClass ct = pool.makeClass(oldClass.getName() + "$Proxy", oldClass); //这里需要生成一个新类，并且继承自原来的类
            CtClass superCt = pool.get(RequestHandler.class.getName());  //需要实现RequestHandler接口
            ct.addInterface(superCt);

            //添加handler方法，在其中添上switch...case段
//            StringBuilder sb = new StringBuilder("public com.peony.engine.framework.net.code.RetPacket handle(" +
            StringBuilder sb = new StringBuilder("public Object handle(" +
                    "int opcode,Object clientData,com.peony.engine.framework.data.entity.session.Session session) throws Exception{");
            sb.append("Object rePacket=null;");
            sb.append("short opCode = opcode;");//$1.getOpcode();");
            sb.append("switch (opCode) {");
            Iterator<Map.Entry<Short, Method>> ite = opMethods.entrySet().iterator();
            while (ite.hasNext()) {
                Map.Entry<Short, Method> entry = ite.next();
                sb.append("case ").append(entry.getKey()).append(":");
                sb.append("rePacket=").append(entry.getValue().getName()).append("(("+entry.getValue().getParameterTypes()[0].getName()+")$2,$3);"); //注意，这里所有的方法都必须是protected或者是public的，否则此部生成会出错
                sb.append("break;");
                //opcodes.add(entry.getKey());
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

    // 生成event的处理类
    private static Class generateEventListenerHandlerClass(Class clazz, Class<?> oriClass) throws Exception {
        Map<Short, List<String>> opMethods = new TreeMap<Short, List<String>>();
        Map<Short, List<String>> opSynMethods = new TreeMap<Short, List<String>>(); // 同步事件
        Method[] methods = oriClass.getDeclaredMethods();
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
                if (parameterTypes[0] != EventData.class) {
                    throw new IllegalStateException("Method " + method.getName() + " Parameter Error");
                }
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
                StringBuilder sb = new StringBuilder("public void handle(" +
                        "com.peony.engine.framework.control.event.EventData eventData) throws Exception{");
                sb.append("short event = $1.getEvent();");//$1.getOpcode();");
                sb.append("switch (event) {");
                Iterator<Map.Entry<Short, List<String>>> ite = opMethods.entrySet().iterator();
                while (ite.hasNext()) {
                    Map.Entry<Short, List<String>> entry = ite.next();
                    sb.append("case ").append(entry.getKey()).append(":");
                    for (String meName : entry.getValue()) {
                        sb.append(meName).append("($1);"); //注意，这里所有的方法都必须是protected或者是public的，否则此部生成会出错
                    }
                    sb.append("break;");
                    //opcodes.add(entry.getKey());
                }
                sb.append("}");
                sb.append("}");
                CtMethod method = CtMethod.make(sb.toString(), ct);
                ct.addMethod(method);
            }
            if(opSynMethods.size() > 0) {
                //添加handler方法，在其中添上switch...case段
                StringBuilder sb = new StringBuilder("public void handleSyn(" +
                        "com.peony.engine.framework.control.event.EventData eventData) throws Exception{");
                sb.append("short event = $1.getEvent();");//$1.getOpcode();");
                sb.append("switch (event) {");
                Iterator<Map.Entry<Short, List<String>>> ite = opSynMethods.entrySet().iterator();
                while (ite.hasNext()) {
                    Map.Entry<Short, List<String>> entry = ite.next();
                    sb.append("case ").append(entry.getKey()).append(":");
                    for (String meName : entry.getValue()) {
                        sb.append(meName).append("($1);"); //注意，这里所有的方法都必须是protected或者是public的，否则此部生成会出错
                    }
                    sb.append("break;");
                    //opcodes.add(entry.getKey());
                }
                sb.append("}");
                sb.append("}");
                CtMethod method = CtMethod.make(sb.toString(), ct);
                ct.addMethod(method);
            }
            return ct.toClass();
        } else {
            return clazz;
        }
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
            StringBuilder sb = new StringBuilder("public com.peony.engine.framework.control.netEvent.NetEventData handle(" +
                    "com.peony.engine.framework.control.netEvent.NetEventData netEventData) throws Exception{");
            sb.append("com.peony.engine.framework.control.netEvent.NetEventData rePacket=null;");
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
                if (op.isAsynchronous() && op.cycle() == -1 && op.cronExpression().length() == 0) {
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
}