package com.peony.cluster.servicerole.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.RoleNotifier;
import com.peony.cluster.servicerole.ServiceRole;
import com.peony.common.exception.MMException;
import com.peony.common.tool.helper.ConfigHelper;
import com.peony.common.tool.util.Util;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 通过nacos配置,yaml格式
 * 服务的配置规则：
 * service: # service跟节点，配置service角色
 *     com.peony.peony.peonydemo.bag.BagService: # 标识service的key，可以是service对应的类。也可以引用service组(service-group)，引用格式为"group@组名字"
 *         provider: # 配置该service对应的提供者，这个的提供者必须是相对于消费者(consumer)而言的，被消费者远程调用。【provider和consumer必须同时配置，或者不配置】
 *             include: ['group@name1', '127.0.0.5'] # 包含的地址数组，地址可以引用地址组(addr-group)，引用为"group@组名字"
 *             exclude: ['group@name2'] # 排除的地址数组，这里排除指从include中排除，地址可以引用地址组(addr-group)，引用格式为"group@组名字"
 *         consumer: # 配置该service对应的消费者，这个的消费者必须是相对于提供者(provider)而言的，被去调用提供者。【provider和consumer必须同时配置，或者不配置】
 *             include: ['group@name2']
 *             exclude:
 *         run_self: # 配置该service是否在自己机器上运行，提供给自己调用。【注意：如果一个父的provider和consumer和run_self都没有配置时，service是在所有机器都是run_self】
 *             include: ['127.0.0.4']
 *             exclude:
 *     group@name1: # 可以引用service组(service-group)，引用格式为"group@组名字"
 *         run_self:
 *             include: ['group@name1']
 *             exclude: ['127.0.0.3']
 * addr-group: # 地址的组根节点，可以将地址分组配置
 *     name1: ['group@name2','172.17.0.2','172.17.0.3'] # 地址的组，可以引用组，注意不要循环引用
 *     name2: ['127.0.0.1']
 * service-group: # service的组根节点，可以将service分组配置
 *     name1: ['com.peony.peony.peonydemo.user.UserService', 'group@name2'] # service的组，可以引用组，注意不要循环引用
 *     name2: ['com.peony.peony.peonydemo.user.UserService3', 'com.peony.peony.peonydemo.user.UserService5']
 *
 *
 * 1、不需要的节点可以不配置。
 * 2、provider：和consumer必须同时配置或不配置，consumer对应的机器对于该服务的调用会调用到provider对应的机器
 * 3、如果调用了未启动的服务，应该报错
 *
 * @author xuerong
 * @since 2020/9/28
 */
public class NacosServiceRoleConfig implements IServiceRoleConfig {
    private static final Logger log = LoggerFactory.getLogger(NacosServiceRoleConfig.class);

    private static final String SERVICE_KEY = "service";
    private static final String PROVIDER_KEY = "provider";
    private static final String CONSUMER_KEY = "consumer";
    private static final String RUN_SELF_KEY = "run_self";
    private static final String GROUP_PARAM_PREFIX = "group@";
    private static final String INCLUDE_KEY = "include";
    private static final String EXCLUDE_KEY = "exclude";
    private static final String ADDR_GROUP_KEY = "addr-group";
    private static final String SERVICE_GROUP_KEY = "service-group";

    private RoleNotifier roleNotifier;
    private volatile Map<String, ServiceRole> serviceRoleMap = new HashMap<>();

    private volatile boolean init = false;

    private String serverAddr = "localhost";
    private String dataId = "peony.service.config";
    private String group = "peony.service.config";

    /**
     * 组
     */
    protected Map<String, Set<String>> groupAddrs = new HashMap<>();

    public synchronized void init() {
        String nocosAddress = ConfigHelper.getString("service.config.nocos.address");
        if (StringUtils.isEmpty(nocosAddress)) {
            throw new MMException("no service.config.nocos.address find in config!");
        }
        log.info("find service.config.nocos.address in config, value = {}", nocosAddress);
        this.serverAddr = nocosAddress;

        try {
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
            ConfigService configService = NacosFactory.createConfigService(properties);
            String serviceConfigContent = configService.getConfig(dataId, group, 5000);
            serviceRoleMap = parseConfig(serviceConfigContent);
            //注册监听
            configService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    synchronized (NacosServiceRoleConfig.this) {
                        Map<String, ServiceRole> serviceRoleMap = parseConfig(configInfo);
                        // 过滤
                        Map<String, ServiceRole> modifyServiceRoleMap = new HashMap<>();
                        for (Map.Entry<String, ServiceRole> entry : serviceRoleMap.entrySet()) {
                            ServiceRole serviceRole = NacosServiceRoleConfig.this.serviceRoleMap.get(entry.getKey());
                            if (serviceRole == entry.getValue()) {
                                continue;
                            }
                            modifyServiceRoleMap.put(entry.getKey(), entry.getValue());
                        }
                        //
                        NacosServiceRoleConfig.this.serviceRoleMap = serviceRoleMap;
                        if (NacosServiceRoleConfig.this.roleNotifier != null) {
                            NacosServiceRoleConfig.this.roleNotifier.notifier(modifyServiceRoleMap);
                        }
                    }
                }

                @Override
                public Executor getExecutor() {
                    return null;
                }
            });
        } catch (NacosException e) {
            throw new MMException("init service config center error[NacosServiceRoleConfig]! service.config.nocos.address = {}", serverAddr, e);
        }
    }

    private String toStringForLog() {
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
        if (!init) {
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

    /**
     * 对内容进行解析，这里支持地址组(addr group)和服务组(service group)
     * 如果需要其他的解析方式，可以重写改方法
     *
     * @param content
     * @return
     */
    protected Map<String, ServiceRole> parseConfig(String content) {
        Map<String, ServiceRole> serviceRoleMap = new HashMap<>();
        if (StringUtils.isEmpty(content)) {
            log.warn("service content is empty from nacos, nacos info = [{}]", toStringForLog());
            return serviceRoleMap;
        }
        log.info("parse config from nacos, content = {}", content);
        Set<String> selfIps = Util.getLocalIPs();
        if (selfIps.isEmpty()) {
            log.error("local ip is empty!");
            return serviceRoleMap;
        }

        Yaml yaml = new Yaml();
        Map<String, Map<String, Object>> map = yaml.load(content);
        Map<String, Object> addrGroup = map.get(ADDR_GROUP_KEY);
        Map<String, Object> serviceGroup = map.get(SERVICE_GROUP_KEY);
        Map<String, Object> serviceMap = map.get(SERVICE_KEY);
        here:
        for (Map.Entry<String, Object> itemEntry : serviceMap.entrySet()) {
            String service = itemEntry.getKey();
            Map<String, Object> contentMap = (Map<String, Object>) itemEntry.getValue();

            Map<String, Object> providerAll = (Map<String, Object>) contentMap.get(PROVIDER_KEY);
            Map<String, Object> consumerAll = (Map<String, Object>) contentMap.get(CONSUMER_KEY);
            Map<String, Object> runSelfAll = (Map<String, Object>) contentMap.get(RUN_SELF_KEY);

            if(isIn(providerAll, selfIps, addrGroup)){
                putServiceRoleMap(serviceRoleMap, service, ServiceRole.Provider, serviceGroup);
            }else if(isIn(consumerAll, selfIps, addrGroup)){
                putServiceRoleMap(serviceRoleMap, service, ServiceRole.Consumer, serviceGroup);
            }else if(isIn(runSelfAll, selfIps, addrGroup)){
                putServiceRoleMap(serviceRoleMap, service, ServiceRole.RunSelf, serviceGroup);
            }else{
                boolean providerEmpty = checkIpConfigEmpty(providerAll);
                boolean consumerEmpty = checkIpConfigEmpty(consumerAll);
                boolean runInSelfEmpty = checkIpConfigEmpty(runSelfAll);
                if(runInSelfEmpty){
                    if(providerEmpty && consumerEmpty){
                        putServiceRoleMap(serviceRoleMap, service, ServiceRole.RunSelf, serviceGroup);
                    }else{
                        putServiceRoleMap(serviceRoleMap, service, ServiceRole.None, serviceGroup);
                    }
                }else{
                    putServiceRoleMap(serviceRoleMap, service, ServiceRole.None, serviceGroup);
                }
            }
        }

        log.info("parse result:{}", serviceRoleMap);

        return serviceRoleMap;
    }

    private void putServiceRoleMap(Map<String, ServiceRole> serviceRoleMap,
                                   String oriService, ServiceRole serviceRole, Map<String, Object> serviceGroup){
        if(!oriService.startsWith(GROUP_PARAM_PREFIX)){
            serviceRoleMap.put(oriService, serviceRole);
            return;
        }
        String name = oriService.substring(GROUP_PARAM_PREFIX.length());
        Object obj = serviceGroup.get(name);
        if(obj == null || !(obj instanceof List)){
            log.error("service group config error! value = {}", obj);
            return;
        }
        List<String> services = (List<String>)obj;
        for(String service : services){
            putServiceRoleMap(serviceRoleMap, service, serviceRole, serviceGroup);
        }
    }

    private Set<String> getAddresses(List<String> ori, Map<String, Object> groups, Set<String> parsedAddr) {
        if (CollectionUtils.isEmpty(ori)) {
            return new HashSet<>();
        }
        Set<String> ret = new HashSet<>();
        for (String addr : ori) {
            if (addr.startsWith(GROUP_PARAM_PREFIX)) {
                // 是个组
                String groupName = addr.substring(GROUP_PARAM_PREFIX.length());
                if(parsedAddr.contains(groupName)){
                    parsedAddr.add(groupName);
                    // 发送了循环引用
                    log.warn("find cycle reference: {}", parsedAddr);
                    continue;
                }
                parsedAddr.add(groupName);
                //
                Set<String> set = groupAddrs.get(groupName);
                if (set == null) {
                    set = getAddresses((List<String>) groups.get(groupName), groups, parsedAddr);
                    groupAddrs.put(groupName, set);
                }
                ret.addAll(set);
            } else {
                ret.add(addr);
            }
        }
        return ret;
    }

    private boolean checkIpConfigEmpty(Map<String, Object> providerAll) {
        if (MapUtils.isEmpty(providerAll)) {
            return true;
        }
        Object obj = providerAll.get(INCLUDE_KEY);
        if (obj == null) {
            return true;
        }
        List<String> oriInclude = (List<String>) obj;
        if (oriInclude.isEmpty()) {
            return true;
        }
        return false;
    }

    private Set<String> checkAndGetList(Object includeObj, Map<String, Object> groups) {
        List<String> oriInclude = new ArrayList<>();
        if (includeObj instanceof List) {
            oriInclude = (List<String>) includeObj;
        }
        Set<String> includes = getAddresses(oriInclude, groups, new HashSet<>());
        return includes;
    }

    private boolean isIn(Map<String, Object> oriAll, Set<String> selfIps, Map<String, Object> groups) {
        if (MapUtils.isEmpty(oriAll)) {
            return false;
        }

        Set<String> includes = checkAndGetList(oriAll.get(INCLUDE_KEY), groups);
        Set<String> excludes = checkAndGetList(oriAll.get(EXCLUDE_KEY), groups);

        // 如果在include且不在exclude中，则认为是true。否则为false
        // 先判断是否在exclude中
        for (String selfIp : selfIps) {
            if (excludes.contains(selfIp)) {
                return false;
            }
        }
        for (String selfIp : selfIps) {
            if (includes.contains(selfIp)) {
                return true;
            }
        }
        return false;
    }
}
