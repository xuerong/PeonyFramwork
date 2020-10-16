package com.peony.cluster.servicerole.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.RoleNotifier;
import com.peony.cluster.servicerole.ServiceRole;
import com.peony.common.tool.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 通过nacos配置
 * 服务的配置规则：
 * 1、以服务名【service名】为key，配置该服务的运行相关服务器
 * 2、配置的表的结构为：
 * ---------------------------------------------------------------
 * id   |   service |   provider    |   consumer    |   run_self
 * ---------------------------------------------------------------
 * id：服务对应的id
 * service：服务key，为包含包路径的service的类名
 * provider：和consumer必须同时配置或不配置，consumer对应的机器对于该服务的调用会调用到provider对应的机器
 * run_self：配置的机器调用该服务会调用自身，run_self（provider也没有配置）没有配置的机器，不能调用该服务（调用会报错，这一点要做到启动时的检查里面）
 * run_self:没有配置的时候。如果provider和consumer也没有配置，则认为包含所有机器。如果provider和consumer有配置，则认为排除所有机器
 * 3、配置主机地址，可以以包含的方式（以"+:"为前缀或者不加前缀），未配置的为不包含，也可以以排除的方式（以"-:"为前缀），未配置的为包含。多个地址，使用逗号(",")隔开，允许换行和空格
 * 4、为方便配置，可以配置“服务组”或者“主机地址组”，并以“服务组”为service，以“主机地址组”为地址值
 * <p>
 * 例如：一个调用链的形式：com.Service1(0.0.0.11,0.0.0.12)->com.Service2(0.0.0.2)->com.Service3(0.0.0.3)
 * ---------------------------------------------------------------
 * id   |   service        |   provider        |   consumer    |   run_self
 * 1    |   com.Service1                                            0.0.0.11,0.0.0.12
 * 2    |   com.Service2        0.0.0.2         0.0.0.11,0.0.0.12   -:0.0.0.3
 * 3    |   com.Service3        0.0.0.3             0.0.0.2         -:0.0.0.11,0.0.0.12
 * ---------------------------------------------------------------
 *
 * @author xuerong
 * @since 2020/9/28
 */
public class NacosServiceRoleConfig implements IServiceRoleConfig {
    private static final Logger log = LoggerFactory.getLogger(NacosServiceRoleConfig.class);

    private RoleNotifier roleNotifier;
    private volatile Map<String, ServiceRole> serviceRoleMap = new HashMap<>();

    private volatile boolean init = false;

    private String serverAddr = "localhost";
    private String dataId = "spring-boot-dubbo-nacos-sample";
    private String group = "mapping-com.gupaoedu.book.nacos.IHelloService";

    public synchronized void init(){
        try{
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
            ConfigService configService = NacosFactory.createConfigService(properties);
            String serviceConfigContent = configService.getConfig(dataId, group, 5000);
            serviceRoleMap = parseConfig(serviceConfigContent);
            //注册监听
            configService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    synchronized (NacosServiceRoleConfig.this){
                        Map<String, ServiceRole> serviceRoleMap = parseConfig(configInfo);
                        // 过滤
                        Map<String, ServiceRole> modifyServiceRoleMap = new HashMap<>();
                        for(Map.Entry<String, ServiceRole> entry : serviceRoleMap.entrySet()){
                            ServiceRole serviceRole = NacosServiceRoleConfig.this.serviceRoleMap.get(entry.getKey());
                            if(serviceRole == entry.getValue()){
                                continue;
                            }
                            modifyServiceRoleMap.put(entry.getKey(), entry.getValue());
                        }
                        //
                        NacosServiceRoleConfig.this.serviceRoleMap = serviceRoleMap;
                        if(NacosServiceRoleConfig.this.roleNotifier != null){
                            NacosServiceRoleConfig.this.roleNotifier.notifier(modifyServiceRoleMap);
                        }
                    }
                }

                @Override
                public Executor getExecutor() {
                    return null;
                }
            });
        }catch (NacosException e){
            log.error("",e);
        }
    }

    private String toStringForLog(){
        return new StringBuilder("serverAddr:").append(serverAddr)
                .append(",dataId:").append(dataId)
                .append(",group:").append(group)
                .toString();
    }

    /**
     * 从nacos中拿取配置，并
     *
     * @return 服务名-角色id
     */
    @Override
    public synchronized Map<String, ServiceRole> getServiceRoles() {
        if(!init){
            init();
        }
        return serviceRoleMap;
    }

    /**
     * 利用nacos的配置通知机制实现服务角色配置的通知
     *
     * @param roleNotifier 通知器
     */
    @Override
    public void subscribe(RoleNotifier roleNotifier) {
        this.roleNotifier = roleNotifier;
    }

    private Map<String, ServiceRole> parseConfig(String content) {
        Map<String, ServiceRole> serviceRoleMap = new HashMap<>();
        if(StringUtils.isEmpty(content)){
            log.warn("service content is empty from nacos, nacos info = [{}]",toStringForLog());
            return serviceRoleMap;
        }
        System.out.println(content);

        Set<String> selfIps = Util.getLocalIPs();
        System.out.println(selfIps);

        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> map = yaml.load(content);

        here:
        for (Map.Entry<String, Map<String, String>> itemEntry : map.entrySet()) {
            String service = itemEntry.getKey();
            Map<String, String> contentMap = itemEntry.getValue();
            String providerAll = contentMap.get("provider");
            String consumerAll = contentMap.get("consumer");
            String runSelfAll = contentMap.get("run_self");

            if(isIn(providerAll, selfIps)){
                serviceRoleMap.put(service, ServiceRole.Provider);
            }else if(isIn(consumerAll, selfIps)){
                serviceRoleMap.put(service, ServiceRole.Consumer);
            }else if(isIn(runSelfAll, selfIps)){
                serviceRoleMap.put(service, ServiceRole.RunSelf);
            }else{
                if(StringUtils.isEmpty(runSelfAll)){
                    if(StringUtils.isEmpty(providerAll) && StringUtils.isEmpty(consumerAll)){
                        serviceRoleMap.put(service, ServiceRole.RunSelf);
                    }else{
                        serviceRoleMap.put(service, ServiceRole.None);
                    }
                }else{
                    serviceRoleMap.put(service, ServiceRole.None);
                }
            }
        }
        return serviceRoleMap;
    }

    private boolean isIn(String providerAll, Set<String> selfIps){
        if(StringUtils.isEmpty(providerAll)){
            return false;
        }
        if(providerAll.startsWith("-:")){
            ServiceRole serviceRole = ServiceRole.Provider;
            providerAll = providerAll.substring(2);
            String[] providers = providerAll.split(",");
            for (String provider : providers) {
                if (selfIps.contains(provider)) {
                    serviceRole = null;
                    break;
                }
            }
            if(serviceRole != null){
                return true;
            }
        }else{
            if(providerAll.startsWith("+:")){
                providerAll = providerAll.substring(2);
            }
            String[] providers = providerAll.split(",");
            for (String provider : providers) {
                if (selfIps.contains(provider)) {
                    return true;
                }
            }
        }
        return false;
    }
}
