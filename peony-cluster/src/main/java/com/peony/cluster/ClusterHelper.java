package com.peony.cluster;

import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.ServiceRole;
import com.peony.common.exception.MMException;
import com.peony.common.tool.util.Util;
import com.peony.core.control.BeanHelper;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 集群服务：开发与部署分离，提供一个部署时可配置的集群部署能力
 * 基本思路：
 * 1、通过dubbo作为rpc框架，并利用其负载均衡和容错能力
 * 2、Service通过代理的方式和dubbo对接，在四种场景中存在：
 * 1)、系统启动时，从注册中心拿取需要远程的服务，构造对应的服务对应的的dubbo接口，继承该服务生成代理类，内部调用dubbo接口的方法，放入Bean容器
 * 2)、系统运行时，收到注册中心新的需要远程的服务，构造对应的服务对应的的接口，并集成该服务和接口生成代理类，重新注入到容器中
 * 3)、系统启动时，从注册中心拿取自己需要提供给别人的服务，构造对应的服务对应的的dubbo接口，继承该服务和接口生成代理类，代理类作为dubbo实现类，并放入Bean容器
 * 4)、系统运行时，收到注册中心新的需要提供给别人远程的服务，构造对应的服务对应的的dubbo接口，继承该服务和接口生成代理类，代理类作为dubbo实现类，重新注入到容器中
 * 3、配置中心，在mmserver.properties中配置，默认为服务列表的第一个。（我这边需要做一个简单的嵌入式的配置中心）
 * <p>
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
    private static IServiceRoleConfig serviceRoleConfig;

    private static Map<Class<?>, ServiceRole> serviceRoleMap = new HashMap<>();

    public static void init() {
        // 从配置中心拉取
        Map<String, ServiceRole> serviceRoleMap = serviceRoleConfig.getServiceRoles();
        handleServiceRoleOfConfig(serviceRoleMap,true);
        serviceRoleConfig.subscribe((roleMap)->{
            handleServiceRoleOfConfig(roleMap,false);
        });
    }
    private static void handleServiceRoleOfConfig(Map<String, ServiceRole> serviceRoleMap,boolean init){
        Map<Class<?>, Object>  serviceBeans = BeanHelper.getServiceBeans();
        for (Map.Entry<String, ServiceRole> entry : serviceRoleMap.entrySet()) {
            try {

                Class<?> cls = Class.forName(entry.getKey());
                Object bean = serviceBeans.get(cls);
                if(bean == null){
                    logger.warn("service bean is not exist , config may error! key = {}",entry.getKey());
                    continue;
                }
                ServiceRole old = ClusterHelper.serviceRoleMap.put(cls, entry.getValue());
                if(!init){
                    if(old == entry.getValue()){
                        continue;
                    }
                    // TODO 如果不是初始化，需要替换旧的
                    if(old == null){
                        // TODO 按照现在role处理
                        continue;
                    }
                    switch (old){
                        case None: // TODO 移除掉
                            break;
                        case Provider:break; // 重新生成，并提供
                        case Consumer:break; // 重新生成
                        case RunSelf:break; // 重新生成
                    }
                }else{
                    Object newBean = parseService(cls,bean);
                    if(newBean == null){
                        //
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.error("class is not exist on cluster init,key = {}", entry.getKey(), e);
            } catch (NoSuchFieldException | CannotCompileException | InstantiationException | NotFoundException | IllegalAccessException e){
                logger.error("class parse fail on cluster init,key = {}", entry.getKey(), e);
            }
        }
    }

    static Object parseService(Class<?> serviceClass,Object bean) throws NoSuchFieldException, CannotCompileException, InstantiationException, NotFoundException, IllegalAccessException {
        ServiceRole serviceRole = serviceRoleMap.get(serviceClass);
        if(serviceRole == null){
            // 没有任何配置，普通代理
            return bean;
        }
        switch (serviceRole){
            case None:
                // 不在本机上对该服务提供能力
                return null;
            case RunSelf:
                // 本机是该服务的自提供者之一
                return bean;
            case Consumer:{
                // 本机是该服务的消费者之一
                bean = ConsumerGenerator.generateConsumer(bean);
                return bean;
            }
            case Provider:{
                // 本机是该服务的提供者之一
                bean = ProviderGenerator.generateProvider(bean);
                return bean;
            }
        }
        throw new MMException("service role error! serviceRole = {}",serviceRole);
    }

    // TODO 这种获取IP的方式不对
    private static String getIp() {
        String ip = Util.getHostAddress();
        return ip;
    }




}
