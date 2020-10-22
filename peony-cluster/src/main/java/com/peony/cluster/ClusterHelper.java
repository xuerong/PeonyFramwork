package com.peony.cluster;

import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.ServiceRole;
import com.peony.cluster.servicerole.impl.DefaultServiceRoleConfig;
import com.peony.cluster.servicerole.impl.NacosServiceRoleConfig;
import com.peony.common.exception.MMException;
import com.peony.common.tool.helper.ConfigHelper;
import com.peony.common.tool.util.Util;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * 集群服务：开发与部署分离，提供一个部署时可配置的集群部署能力
 * 基本思路：
 * 1、通过dubbo作为rpc框架，并利用其负载均衡和容错能力
 * 2、Service通过代理的方式和dubbo对接，在四种场景中存在：
 * 1)、系统启动时，从注册中心拿取需要远程的服务，构造对应的服务对应的的dubbo接口，继承该服务生成代理类，内部调用dubbo接口的方法，放入Bean容器
 * 2)、系统运行时，收到注册中心新的需要远程的服务，构造对应的服务对应的的接口，并集成该服务和接口生成代理类，重新注入到容器中
 * 3)、系统启动时，从注册中心拿取自己需要提供给别人的服务，构造对应的服务对应的的dubbo接口，继承该服务和接口生成代理类，代理类作为dubbo实现类，并放入Bean容器
 * 4)、系统运行时，收到注册中心新的需要提供给别人远程的服务，构造对应的服务对应的的dubbo接口，继承该服务和接口生成代理类，代理类作为dubbo实现类，重新注入到容器中
 * 3、配置中心，在mmserver.properties中配置，默认为服务列表的第一个。（我这边需要做一个简单的嵌入式的配置中心）
 *
 * <p>
 * 服务的配置规则：
 * 继承IServiceRoleConfig接口，实现服务配置的初始获取和变化通知
 * 每个服务在本机运行情况包含四种，参考{@link ServiceRole}
 * 对于没有配置的，默认是本机运行
 * <p>
 * 当服务类型变化的时候：
 * 1、没有出现再变化列表的，就等于没有变化：这里注意一点，对于由有配置变成没有配置，属于变化，需要带进来
 * 2、对于Updatable注解，对于None和Consumer，要关闭
 * 3、初始化的时候，就要先判断服务本机是否运行，否则，不初始化
 * 4、发生变化的时候，重新生成一个，替换旧的。如果是移除，则移除
 *
 * @author zhengyuzhen
 * @since 2020/9/22
 */
public class ClusterHelper {
    private static Logger logger = LoggerFactory.getLogger(ClusterHelper.class);

    private static IServiceRoleConfig serviceRoleConfig = new DefaultServiceRoleConfig();

    private static Map<Class<?>, ServiceRole> serviceRoleMap = new HashMap<>();

    private static List<RegistryConfig> registryConfigs;

    private static volatile boolean init = false;

    /**
     * 这里提供从IServiceRoleConfig拿取服务信息，并转换的能力
     *
     * @param clusterBeanCallback 当配置变化的时候，回调
     */
    public static synchronized void init(ClusterBeanCallback clusterBeanCallback) {
        if(init){
            logger.error("have initialized");
            return;
        }
        init = true;
        String registryAddressesStr = ConfigHelper.getString("dubbo.registry.address");
        if(StringUtils.isEmpty(registryAddressesStr)){
            registryAddressesStr = "nacos://127.0.0.1:8848";
            logger.info("no dubbo.registry.address in config, use default value : {}", registryAddressesStr);
        }else{
            logger.info("find dubbo.registry.address in config, value = {}", registryAddressesStr);
        }

        String[] registryAddresses = registryAddressesStr.split("\\|");
        registryConfigs = new ArrayList<>();
        for(String registryAddress : registryAddresses){
            RegistryConfig registry = new RegistryConfig();
            registry.setProtocol("dubbo");
            registry.setAddress(registryAddress);
            registry.setGroup("generated-by-peony");
            registryConfigs.add(registry);
        }

        String serviceRoleConfigClassConfig = ConfigHelper.getString("service.role.config.class");
        if(StringUtils.isNotEmpty(serviceRoleConfigClassConfig)){
            logger.info("find service.role.config.class in config, value = {}", serviceRoleConfigClassConfig);
            try{
                Class cls = Class.forName(serviceRoleConfigClassConfig);
                serviceRoleConfig = (IServiceRoleConfig)cls.getDeclaredConstructor().newInstance();
            }catch (ClassNotFoundException e){
                logger.error("service role config class error! config = {}", serviceRoleConfigClassConfig, e);
            }catch (NoSuchMethodException e){
                logger.error("service role config class constructor params error! config = {}", serviceRoleConfigClassConfig, e);
            }catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                logger.error("service role config class newInstance error! config = {}", serviceRoleConfigClassConfig, e);
            }catch (ClassCastException e){
                logger.error("service role config class type error! config = {}", serviceRoleConfigClassConfig, e);
            }
        }else{
            logger.warn("the service role configuration class is not configured , use default[{}]", serviceRoleConfig.getClass().getName());
        }
        // 从配置中心拉取
        Map<String, ServiceRole> serviceRoleMap = serviceRoleConfig.getServiceRoles();
        if(serviceRoleMap == null){
            logger.warn("service role map is empty , service role config class is {}", serviceRoleConfig.getClass().getName());
        }else{
            transServiceClass(serviceRoleMap);
        }
        serviceRoleConfig.subscribe((roleMap) -> {
            Map<Class<?>,ServiceRole> modify = transServiceClass(roleMap);
            if(modify == null){
                logger.warn("service role modify map is empty , service role config class is {}", serviceRoleConfig.getClass().getName());
            }else{
                clusterBeanCallback.handle(modify);
            }
        });
    }

    private static synchronized Map<Class<?>,ServiceRole> transServiceClass(Map<String, ServiceRole> serviceRoleMap){
        Map<Class<?>,ServiceRole> modify = new HashMap<>();
        for (Map.Entry<String, ServiceRole> entry : serviceRoleMap.entrySet()) {
            try {
                Class<?> cls = Class.forName(entry.getKey());
                ServiceRole serviceRole = ClusterHelper.serviceRoleMap.get(cls);
                if(serviceRole == null || serviceRole != entry.getValue()){
                    ClusterHelper.serviceRoleMap.put(cls,entry.getValue());
                    modify.put(cls,entry.getValue());
                }
            }catch (ClassNotFoundException e) {
                logger.error("class is not exist on cluster init,key = {}", entry.getKey(), e);
            }
        }
        /**
         * 检查 引用关系，
         * 1、provider和run_self中不能使用none
         */
        for(Map.Entry<Class<?>, ServiceRole> entry : ClusterHelper.serviceRoleMap.entrySet()){
            if(entry.getValue() == ServiceRole.Consumer || entry.getValue() == ServiceRole.None){
                continue;
            }
            Field[] fields = entry.getKey().getDeclaredFields();
            for(Field field : fields) {
                ServiceRole serviceRole = ClusterHelper.serviceRoleMap.get(field.getClass());
                if(serviceRole == null){
                    continue;
                }
                if(serviceRole == ServiceRole.None){
                    // 这里暂时只打印日志
                    logger.error("service reference error! type {}[{}] ref to type None",
                            entry.getValue(), entry.getKey().getName());
                }
            }
        }
        return modify;
    }

    public static synchronized Object parseService(Class<?> serviceClass, Object bean) throws NoSuchFieldException, CannotCompileException, InstantiationException, NotFoundException, IllegalAccessException {
        ServiceRole serviceRole = serviceRoleMap.get(serviceClass);
        if (serviceRole == null) {
            // 没有任何配置，普通代理
            return bean;
        }
        switch (serviceRole) {
            case None:
                // 不在本机上对该服务提供能力
                return null;
            case RunSelf:
                // 本机是该服务的自提供者之一
                return bean;
            case Consumer: {
                // 本机是该服务的消费者之一
                bean = ConsumerGenerator.generateConsumer(serviceClass, bean, registryConfigs);
                return bean;
            }
            case Provider: {
                // 本机是该服务的提供者之一
                bean = ProviderGenerator.generateProvider(serviceClass, bean, registryConfigs);
                return bean;
            }
        }
        throw new MMException("service role error! serviceRole = {}", serviceRole);
    }

    public synchronized static Map<Class<?>, ServiceRole> getServiceRoleMap() {
        if(!init){
            throw new MMException("cluster helper have not init!");
        }
        return serviceRoleMap;
    }

}
