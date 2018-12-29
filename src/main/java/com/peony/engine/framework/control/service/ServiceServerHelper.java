package com.peony.engine.framework.control.service;

import com.google.api.client.util.Maps;
import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.netEvent.remote.RemoteCallService;
import com.peony.engine.framework.control.rpc.IRoute;
import com.peony.engine.framework.control.rpc.Remotable;
import com.peony.engine.framework.control.rpc.RouteType;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.tool.helper.ClassHelper;
import com.peony.engine.framework.tool.helper.ConfigHelper;
import com.peony.engine.framework.tool.util.Util;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class ServiceServerHelper {
    private static final Logger log = LoggerFactory.getLogger(ServiceServerHelper.class);

    //ServiceRunOnServer  ServiceCallRule
    private static Map<String,ServiceRunOnServer> serviceRunOnServerMap = new HashMap<>();
    private static Map<String,ServiceCallRule> serviceCallRuleMap = new HashMap<>();
    private static Map<Class<?>,RunSegment> serviceServerIdMap = new HashMap<>();

    static {
        try {
            //ServiceRunOnServer
            List<Class<?>> ServiceRunOnServerClasses = ClassHelper.getClassListBySuper(ServiceRunOnServer.class);
            for (Class<?> cls : ServiceRunOnServerClasses) {
                ServiceRunOnServer serviceRunOnServer = (ServiceRunOnServer) cls.newInstance();
                serviceRunOnServerMap.put(cls.getName(), serviceRunOnServer);
            }
            //ServiceCallRule
            List<Class<?>> ServiceCallRuleClasses = ClassHelper.getClassListBySuper(ServiceCallRule.class);
            for (Class<?> cls : ServiceCallRuleClasses) {
                ServiceCallRule serviceCallRule = (ServiceCallRule) cls.newInstance();
                serviceCallRuleMap.put(cls.getName(), serviceCallRule);
            }
//            ManagementFactory.getop
        }catch (Throwable e){
            throw new MMException("ServiceServerHelper static error",e);
        }
    }

    public static int getServerId(int serverId,Class<?> service,Object... params){
        RunSegment runSegment = serviceServerIdMap.get(service);
        if(runSegment.serverIdSet.contains(serverId) && !runSegment.selfUseRule){
            return Server.getServerId();
        }
        switch (runSegment.ruleType){
            case Order:
                return runSegment.serverIds[(Integer)params[0]];
            case Random:
                return runSegment.serverIds[Util.randomInt(runSegment.serverIds.length)]; // TODO 这个是否考虑去掉之前随机但是失败过的？
            case Modulus:
                return runSegment.serverIds[serverId%runSegment.serverIds.length];
            case SelfDefined:
                return runSegment.serviceCallRule.getServerId(serverId); // TODO 是否也要考虑之前失败过的
        }
        throw new MMException("getServerId error!serverId={},service={},params={}",serverId,service,params);
    }

//    private static Object onceCall(RemoteCallService remoteCallService,int serverId,Class serviceClass, String methodName, Object[] params){
//        try {
//            // TODO 远程调用异常暂时使用null
//            return remoteCallService.remoteCallSyn(serverId, serviceClass, methodName, params,null);
//        }catch (Throwable e){
//            if(e instanceof MMException){
//                MMException mmException = (MMException)e;
//                if(mmException.getExceptionType() == MMException.ExceptionType.SendNetEventFail){
//                    // 远程调用失败，调用下一个
//                    return null;
//                }else{
//                    throw e; // 远程服务器抛出的异常，直接抛出来
//                }
//            }
//            throw e; // 远程服务器抛出的异常，直接抛出来
//        }
//    }
//
//    public static Object remoteCall(Class serviceClass, String methodName, Object[] params){
//        RunSegment runSegment = serviceServerIdMap.get(serviceClass);
//        if(runSegment == null){
//            throw new MMException("runSegment is not exist!");
//        }
//        RemoteCallService remoteCallService = BeanHelper.getServiceBean(RemoteCallService.class);
//        switch (runSegment.ruleType){
//            case Order:
//                for(int serverId:runSegment.serverIds) {
//                    Object onceCallRet = onceCall(remoteCallService,serverId, serviceClass, methodName, params);
//                    if(onceCallRet != null){
//                        return onceCallRet;
//                    }
//                }
//                break;
//            case Random:
//                int firstServerId = runSegment.serverIds[Util.randomInt(runSegment.serverIds.length)];// TODO 随机是否有效率上的问题，优化之
//                Object onceCallRet = onceCall(remoteCallService,firstServerId, serviceClass, methodName, params);
//                if(onceCallRet != null){
//                    return onceCallRet;
//                }
//
//                if(runSegment.serverIds.length > 1){
//                    List<Integer> failServerIdList = new ArrayList<Integer>(runSegment.serverIds.length){{
//                        for(int serverId:runSegment.serverIds) {
//                            if(serverId != firstServerId) {
//                                add(serverId);
//                            }
//                        }
//                    }};
//                    while(!failServerIdList.isEmpty()){
//                        int serverId = failServerIdList.remove(Util.randomInt(failServerIdList.size()));
//                        onceCallRet = onceCall(remoteCallService,serverId, serviceClass, methodName, params);
//                        if(onceCallRet != null){
//                            return onceCallRet;
//                        }
//                    }
//                }
//                break;
//            case Modulus:
//                int serverId = runSegment.serverIds[Server.getServerId()%runSegment.serverIds.length];
//                onceCallRet = onceCall(remoteCallService,serverId, serviceClass, methodName, params);
//                if(onceCallRet != null){
//                    return onceCallRet;
//                }
//                break;
//            case SelfDefined:
//                firstServerId =runSegment.serviceCallRule.getServerId(Server.getServerId());
//                onceCallRet = onceCall(remoteCallService,firstServerId, serviceClass, methodName, params);
//                if(onceCallRet != null){
//                    return onceCallRet;
//                }
//                serverId = runSegment.serviceCallRule.failGetNextServerId(Server.getServerId(), Arrays.asList(firstServerId));
//                if (serverId > 0){
//                    List<Integer> failIds = new LinkedList<>();
//                    failIds.add(firstServerId);
//                    while(serverId > 0) {
//                        if(failIds.contains(serverId)){
//                            log.error("serviceCallRule error!");
//                            break;
//                        }
//                        onceCallRet = onceCall(remoteCallService, serverId, serviceClass, methodName, params);
//                        if (onceCallRet != null) {
//                            return onceCallRet;
//                        }
//                        failIds.add(serverId);
//                        serverId = runSegment.serviceCallRule.failGetNextServerId(Server.getServerId(), failIds);
//                    }
//                }
//
//        }
//        log.error("remote call error!,all server has called,runSegment={}",runSegment.toString());
//        throw new MMException("remoteCallError!");
//    }

    public static Class<?> genService(Class<?> serviceClass){
        /**
         * 查看该方法是否有在service.properties中配置
         */
        String prefix = "service."+serviceClass.getSimpleName();
        Map<String, String> serviceConfig = ConfigHelper.getServiceMap(prefix);
        if(serviceConfig.size() == 0){
            prefix = "service."+serviceClass.getName();
            serviceConfig = ConfigHelper.getServiceMap(prefix);
        }
        if(serviceConfig.size() == 0){
            return serviceClass;
        }
        // 改成远程调用
                    /*
                    #*************************************************************************************************
#
# service.properties使用说明
#
# 所有的service不能重名
#
#
# 通过配置service运行的服务器，可以对功能进行纵向切分，不同服务器运行不同服务，不配置在此的service将在所有服务器运行。
# 【大部分service如果不进行调用性能消耗很少，我们只需要决定哪些服务必须在哪些服务器上运行即可】
#
# 配置service在哪些服务器上运行和运行规则。如果一个service不在本服务器上运行，那么对其调用将根据运行规则选取一个服务器调用
#
# 1、在哪些服务器上运行，配置运行该service的服务器id
#       多个配置用','隔开，可以有不同的配置方式：
#       1）直接配置：直接配置服务器id，如【1,2,3】
#       2）连续多个服务器：配置一个服务器id区间，如【1-3】
#       3）自定义哪些服务器：继承自ServiceRunOnServer
#       如果在123服务器上运行，则可以如下配置：【1,2,3】或【1-3】或【1,2-3】等
# 2、调用规则，即如何选择运行该服务的服务器
#       包括：
#       1）顺序调用【order】：先调用第一个，如果失败，则调用第二个，以此类推。容错
#       2）随机调用【random】：从配置的服务器中随机选出一个，调用，调用失败将重试。调用平均分布，负载平衡
#       3）取模调用【modulus】：根据自己服务器的id对配置该service的服务器个数取模i，调用第i个服务器。
#       4）自定义规则：继承自ServiceCallRule，根据id获取调用id
# 3、运行该服务的服务器是否使用调用规则：true使用，false不使用，调用自身
#
#***************************************************************************************************



# IdService：全局只能有一个IdService，确保id的唯一性
service.IdService.server = 1
service.IdService.callRule = order
service.IdService.selfUseRule = false
                     */

        Set<Integer> serverIds = new LinkedHashSet<>();

        String serverConfig = serviceConfig.get(prefix+".server");
        System.out.println(serverConfig);
        String callRuleConfig = serviceConfig.get(prefix+".callRule");
        System.out.println(callRuleConfig);
        String selfUseRuleConfig = serviceConfig.get(prefix+".selfUseRule");
        System.out.println(selfUseRuleConfig);

        String[] servers = serverConfig.split(",");
        for(String server : servers){
            if(StringUtils.isNumeric(server)){
                serverIds.add(Integer.parseInt(server));
            }else if(server.indexOf('-')>0){
                String[] serverFromTo = server.split("-");
                if(serverFromTo.length != 2){
                    throw new MMException("service config server error,service = {}",serviceClass.getName());
                }
                int from = Integer.parseInt(serverFromTo[0]);
                int to = Integer.parseInt(serverFromTo[1]);

                for(int i= from;i<= to;i++){
                    serverIds.add(i);
                }
            }else{
                ServiceRunOnServer serviceRunOnServer = serviceRunOnServerMap.get(server);
                if(serviceRunOnServer != null) {
                    int[] addServerIds = serviceRunOnServer.getServerIds();
                    for (int serverId : addServerIds) {
                        serverIds.add(serverId);
                    }
                }else{
                    throw new MMException("service config server error,service = {}",serviceClass.getName());
                }
            }
        }
        if(serverIds.size() == 0 || (serverIds.contains(Server.getServerId()) && !Boolean.parseBoolean(selfUseRuleConfig))){
            return serviceClass;
        }

        int[] serverIdArray = new int[serverIds.size()];
        int i=0;
        for (Integer serverId : serverIds) {
            serverIdArray[i++] = serverId;
        }
        RunSegment runSegment = new RunSegment();
        runSegment.serverIdSet = serverIds;
        runSegment.serverIds = serverIdArray;
        runSegment.ruleType = RuleType.getRuleTypeByConfig(callRuleConfig);
        if(runSegment.ruleType == RuleType.SelfDefined){
            runSegment.serviceCallRule = serviceCallRuleMap.get(callRuleConfig);
        }
        runSegment.selfUseRule = Boolean.parseBoolean(selfUseRuleConfig);

        serviceServerIdMap.put(serviceClass,runSegment);

        // 改成远程调用


        return serviceClass;
    }



    /**
     * 1）顺序调用【order】：先调用第一个，如果失败，则调用第二个，以此类推。容错
     #       2）随机调用【random】：从配置的服务器中随机选出一个，调用，调用失败将重试。调用平均分布，负载平衡
     #       3）取模调用【modulus】：根据自己服务器的id对配置该service的服务器个数取模i，调用第i个服务器。
     #       4）自定义规则：继承自ServiceCallRule，根据id获取调用id
     */
    /**
     * 如果需要远程调用：
     * 如果是第一种，方法本身则需要构建一个循环
     * 如果是第二种，则方法本身需要一个随机
     * 如果是第三种，方法外面就可以生成调用谁
     * 如果是第四种，则需要在方法本身中调用
     *
     *
     * @param pool
     * @param serviceClass
     * @param newServiceClass
     * @return
     * @throws Exception
     */
    private static Class<?> genService(ClassPool pool, Class<?> serviceClass, Class<?> newServiceClass) throws Exception {


        // 只处理 当前类定义的 public 的方法

        CtClass ctClazz = pool.get(serviceClass.getName());
        CtClass cls = pool.makeClass(ctClazz.getName() + "$Proxy", ctClazz);

        // 改写remote方法, 使用远程调用
        for(CtMethod ctMethod:ctClazz.getMethods()) {
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
                paramsStr.append(ServiceHelper.praseBaseTypeStrToObjectTypeStr(paramClass.getName(), "p" + i));
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
                sb.append("return " + ServiceHelper.parseBaseTypeStrToObjectTypeStr(ctMethod.getReturnType().getName()));
            }
            sb.append("}");
            log.info("==============================================\n"+sb.toString());
            CtMethod method = CtMethod.make(sb.toString(), cls);
            cls.addMethod(method);
        }
        newServiceClass = cls.toClass();
        return newServiceClass;
    }



    static class RunSegment{
        Set<Integer> serverIdSet;
        int[] serverIds;
        RuleType ruleType;
        ServiceCallRule serviceCallRule;
        boolean selfUseRule;


        public String toString(){
            StringBuilder sb = new StringBuilder();
            for(int id:serverIds){
                sb.append(id).append(";");
            }
            return sb.append("|").append(ruleType).append("|").append(serviceCallRule).append("|").append(selfUseRule).toString();
        }
    }

    static enum RuleType{
        /**
         * 1）顺序调用【order】：先调用第一个，如果失败，则调用第二个，以此类推。容错
         #       2）随机调用【random】：从配置的服务器中随机选出一个，调用，调用失败将重试。调用平均分布，负载平衡
         #       3）取模调用【modulus】：根据自己服务器的id对配置该service的服务器个数取模i，调用第i个服务器。
         #       4）自定义规则：继承自ServiceCallRule，根据id获取调用id
         */
        Order("order"),
        Random("random"),
        Modulus("modulus"),
        SelfDefined(""),
        ;
        final String id;
        RuleType(String id){
            this.id = id;
        }

        public String getId() {
            return id;
        }

        static RuleType getRuleTypeByConfig(String callRuleConfig){
            switch (callRuleConfig){
                case "order":
                    return Order;
                case "random":
                    return Random;
                case "modulus":
                    return Modulus;
            }
            if(serviceCallRuleMap.containsKey(callRuleConfig)){
                return SelfDefined;
            }
            throw new MMException("rule type error!,config="+callRuleConfig);
        }
    }

}
