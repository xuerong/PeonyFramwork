package com.peony.cluster.servicerole.impl;

import com.peony.cluster.ConsumerGenerator;
import com.peony.cluster.ProviderGenerator;
import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.RoleNotifier;
import com.peony.cluster.servicerole.ServiceConfig;
import com.peony.cluster.servicerole.ServiceRole;

import java.util.HashMap;
import java.util.Map;

/**
 * @author z84150192
 * @since 2020/9/28
 */
public class NacosServiceRoleConfig implements IServiceRoleConfig {
    /**
     * 从nacos中拿取配置，并
     *
     * @return 服务名-角色id
     */
    @Override
    public Map<String, ServiceRole> getServiceRoles() {
        Map<String, ServiceRole> ret = new HashMap<>();
        if(System.getProperty("serverNum").equals("1")){
            ret.put("com.peony.peony.peonydemo.bag.BagService",ServiceRole.Consumer);
        }else{
            ret.put("com.peony.peony.peonydemo.bag.BagService",ServiceRole.Provider);
        }
        return ret;
    }

    /**
     * 利用nacos的配置通知机制实现服务角色配置的通知
     *
     * @param roleNotifier 通知器
     */
    @Override
    public void subscribe(RoleNotifier roleNotifier) {

    }

    // 根据配置，决定XXX
    // TODO 这里需要考虑一点，即使配置中心直接结算出每个机器对该服务的运行情况并告知，还是由每个机器自行计算
    // 告知的话分为四种情况：1、提供者，2、消费者、3、自运行、4、不提供
//    static Object parseService(Class<?> serviceClass) throws Exception{
//        // 决定如何处理！？
//        ServiceConfig serviceConfig = serviceConfigMap.get(serviceClass);
//        if(serviceConfig == null){
//            // 没有任何配置，普通代理
//            return generateNormal(serviceClass);
//        }
//        String localIp = getIp();
//        if(serviceConfig.getProviders() != null && serviceConfig.getProviders().contains(localIp)){
//            // 本机是该服务的提供者之一
//            Object obj = generateNormal(serviceClass);
//            obj = ProviderGenerator.generateProvider(obj);
//            return obj;
//        }
//        if(serviceConfig.getConsumers() != null && serviceConfig.getConsumers().contains(localIp)){
//            // 本机是该服务的消费者之一
//            Object obj = generateNormal(serviceClass);
//            obj = ConsumerGenerator.generateConsumer(obj);
//            return obj;
//        }
//        if(serviceConfig.getRunSelves() != null){
//            if(serviceConfig.getRunSelves().contains(localIp)){
//                // 本机是该服务的自提供者之一
//                return generateNormal(serviceClass);
//            }
//            // 不在本机上对该服务提供能力
//            return null;
//        }
//        // run_self:没有配置的时候。如果provider和consumer也没有配置，则认为包含所有机器。如果provider和consumer有配置，则认为排除所有机器
//        if(serviceConfig.getProviders() == null){
//            // 本机是该服务的自提供者之一
//            return generateNormal(serviceClass);
//        }
//        return null;
//    }
}
