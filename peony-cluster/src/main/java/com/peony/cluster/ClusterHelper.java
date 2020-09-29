package com.peony.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 集群服务：开发与部署分离，提供一个部署时可配置的集群部署能力
 * 基本思路：
 * 1、通过dubbo作为rpc框架，并利用其负载均衡和容错能力
 * 2、Service通过代理的方式和dubbo对接，在四种场景中存在：
 *      1)、系统启动时，从注册中心拿取需要远程的服务，构造对应的服务对应的的dubbo接口，继承该服务生成代理类，内部调用dubbo接口的方法，放入Bean容器
 *      2)、系统运行时，收到注册中心新的需要远程的服务，构造对应的服务对应的的接口，并集成该服务和接口生成代理类，重新注入到容器中
 *      3)、系统启动时，从注册中心拿取自己需要提供给别人的服务，构造对应的服务对应的的dubbo接口，继承该服务和接口生成代理类，代理类作为dubbo实现类，并放入Bean容器
 *      4)、系统运行时，收到注册中心新的需要提供给别人远程的服务，构造对应的服务对应的的dubbo接口，继承该服务和接口生成代理类，代理类作为dubbo实现类，重新注入到容器中
 * 3、配置中心，在mmserver.properties中配置，默认为服务列表的第一个。（我这边需要做一个简单的嵌入式的配置中心）
 *
 * 服务的配置规则：
 * 1、以服务名【service名】为key，配置该服务的运行相关服务器
 * 2、配置的表的结构为：
 * ---------------------------------------------------------------
 *  id   |   service |   provider    |   consumer    |   run_self
 * ---------------------------------------------------------------
 * id：服务对应的id
 * service：服务key，为包含包路径的service的类名
 * provider：和consumer必须同时配置或不配置，consumer对应的机器对于该服务的调用会调用到provider对应的机器
 * run_self：配置的机器调用该服务会调用自身，run_self（provider也没有配置）没有配置的机器，不能调用该服务（调用会报错，这一点要做到启动时的检查里面）
 *      run_self:没有配置的时候。如果provider和consumer也没有配置，则认为包含所有机器。如果provider和consumer有配置，则认为排除所有机器
 * 3、配置主机地址，可以以包含的方式（以"+:"为前缀或者不加前缀），未配置的为不包含，也可以以排除的方式（以"-:"为前缀），未配置的为包含。多个地址，使用逗号(",")隔开，允许换行和空格
 * 4、为方便配置，可以配置“服务组”或者“主机地址组”，并以“服务组”为service，以“主机地址组”为地址值
 *
 * 例如：一个调用链的形式：com.Service1(0.0.0.11,0.0.0.12)->com.Service2(0.0.0.2)->com.Service3(0.0.0.3)
 * ---------------------------------------------------------------
 *  id   |   service        |   provider        |   consumer    |   run_self
 *  1    |   com.Service1                                            0.0.0.11,0.0.0.12
 *  2    |   com.Service2        0.0.0.2         0.0.0.11,0.0.0.12   -:0.0.0.3
 *  3    |   com.Service3        0.0.0.3             0.0.0.2         -:0.0.0.11,0.0.0.12
 * ---------------------------------------------------------------
 *
 *
 * @author zhengyuzhen
 * @since 2020/9/22
 */
public class ClusterHelper {
    private static Map<Class<?>,ServiceConfig> serviceConfigMap = new HashMap<>();
    public static void init(){
        // 从配置中心拉取



    }

    // TODO 这里需要考虑一点，即使配置中心直接结算出每个机器对该服务的运行情况并告知，还是由每个机器自行计算
    // 告知的话分为四种情况：1、提供者，2、消费者、3、自运行、4、不提供
    static Object parseService(Class<?> serviceClass) throws Exception{
        // 决定如何处理！？
        ServiceConfig serviceConfig = serviceConfigMap.get(serviceClass);
        if(serviceConfig == null){
            // 没有任何配置，普通代理
            return generateNormal(serviceClass);
        }
        String localIp = getIp();
        if(serviceConfig.getProviders() != null && serviceConfig.getProviders().contains(localIp)){
            // 本机是该服务的提供者之一
            Object obj = generateNormal(serviceClass);
            obj = ProviderGenerator.generateProvider(obj);
            return obj;
        }
        if(serviceConfig.getConsumers() != null && serviceConfig.getConsumers().contains(localIp)){
            // 本机是该服务的消费者之一
            Object obj = generateNormal(serviceClass);
            obj = ConsumerGenerator.generateConsumer(obj);
            return obj;
        }
        if(serviceConfig.getRunSelves() != null){
            if(serviceConfig.getRunSelves().contains(localIp)){
                // 本机是该服务的自提供者之一
                return generateNormal(serviceClass);
            }
            // 不在本机上对该服务提供能力
            return null;
        }
        // run_self:没有配置的时候。如果provider和consumer也没有配置，则认为包含所有机器。如果provider和consumer有配置，则认为排除所有机器
        if(serviceConfig.getProviders() == null){
            // 本机是该服务的自提供者之一
            return generateNormal(serviceClass);
        }
        return null;
    }
    private static String getIp(){

    }
    private static Object generateNormal(Class<?> serviceClass){
        // TODO 普通代理
    }

    class ServiceConfig{
        private int id;
        private Class<?> service;
        private Addresses providers;
        private Addresses consumers;
        private Addresses runSelves;

        class Addresses{
            private boolean include; // true:include,false:exclude
            private Set<String> addresses;

            public boolean contains(String ip){
                if(addresses == null){
                    return false;
                }
                return include == addresses.contains(ip);
            }

            public boolean isInclude() {
                return include;
            }

            public void setInclude(boolean include) {
                this.include = include;
            }

            public Set<String> getAddresses() {
                return addresses;
            }

            public void setAddresses(Set<String> addresses) {
                this.addresses = addresses;
            }
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Class<?> getService() {
            return service;
        }

        public void setService(Class<?> service) {
            this.service = service;
        }

        public Addresses getProviders() {
            return providers;
        }

        public void setProviders(Addresses providers) {
            this.providers = providers;
        }

        public Addresses getConsumers() {
            return consumers;
        }

        public void setConsumers(Addresses consumers) {
            this.consumers = consumers;
        }

        public Addresses getRunSelves() {
            return runSelves;
        }

        public void setRunSelves(Addresses runSelves) {
            this.runSelves = runSelves;
        }
    }


}
