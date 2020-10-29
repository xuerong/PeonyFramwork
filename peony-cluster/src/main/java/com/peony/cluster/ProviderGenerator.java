package com.peony.cluster;

import com.peony.common.exception.MMException;
import com.peony.common.tool.util.Util;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xuerong
 * @since 2020/9/27
 */
class ProviderGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ProviderGenerator.class);
    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final Condition STOP = LOCK.newCondition();

    static Object generateProvider(Class<?> serviceClass, Object object, List<? extends RegistryConfig> registries) throws NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException {
        if(CollectionUtils.isEmpty(registries)){
            throw new MMException("registries is empty!");
        }
        // 生成代理接口
        Class<?> proxyInterface = DubboHelper.generateProxyInterface(serviceClass);
        // 生成代理类
        Class<?> proxyClass = generateProxy(object,proxyInterface);
        // 生成代理对象
        Object proxyObject = proxyClass.newInstance();
        // 生成对应的dubbo服务提供者
        ServiceConfig serviceConfig = new ServiceConfig();
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20880);

        ProviderConfig provider = new ProviderConfig();
        provider.setExport(true);
        provider.setProtocol(protocolConfig);
        serviceConfig.setProvider(provider);
        ApplicationConfig app = new ApplicationConfig(Util.humpToCenterLine(serviceClass.getSimpleName()));
        serviceConfig.setApplication(app);

        serviceConfig.setRegistries(registries);

        serviceConfig.setGeneric(Boolean.TRUE.toString());

        serviceConfig.setInterface(proxyInterface);

        serviceConfig.setRef(proxyObject);

        serviceConfig.export();

        return proxyObject;
    }

    private static Class<?> generateProxy(Object object,Class<?> proxyInterface) throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()); //获得类池
        CtClass oldClass = pool.get(object.getClass().getName());
        CtClass proxyClazz = pool.makeClass(oldClass.getName() + "$Proxy", oldClass);
        CtClass superCt = pool.get(proxyInterface.getName());  //需要实现
        proxyClazz.addInterface(superCt);
        return proxyClazz.toClass();
    }
}
