package com.peony.cluster;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
 * 4、
 *
 * @author zhengyuzhen
 * @since 2020/9/22
 */
public class ClusterHelper {
    public static void init(){
        //

    }

    private Object generateConsumer(Object object) throws NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        // 生成代理接口
        Class<?> proxyInterface = generateProxyInterface(object);
        // 生成代理类
        Class<?> proxyClass = generateProxy(object,proxyInterface);
        // 生成代理对象
        Object proxyObject = proxyClass.newInstance();
        // 生成接口对应的dubbo代理对象
        String tmpSimpleName = proxyInterface.getSimpleName();
        String proxyInterfaceName = tmpSimpleName.substring(0,1).toLowerCase()
                +tmpSimpleName.substring(1);
        Field field = proxyClass.getDeclaredField(proxyInterfaceName);
        field.setAccessible(true);

        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setUrl("dubbo://127.0.0.1:20880/"+proxyInterface.getName());
        referenceConfig.setInterface(proxyInterface.getName());
        referenceConfig.setId(proxyInterface.getSimpleName());
        ApplicationConfig application = new ApplicationConfig();
        application.setName("order-service");
        referenceConfig.setApplication(application);
        referenceConfig.setGeneric(true);
        Object obj = referenceConfig.get();
        System.out.println(obj);
        // 为代理对象赋值接口对应的dubbo代理对象
        field.set(proxyObject,obj);

        return proxyObject;
    }

    private static Class<?> generateProxy(Object object,Class<?> proxyInterface) throws CannotCompileException, NotFoundException {
        ClassPool pool = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()); //获得类池
        CtClass oldClass = pool.get(object.getClass().getName());
        CtClass proxyClazz = pool.makeClass(oldClass.getName() + "$Proxy", oldClass);
        // 添加接口的引用
        String tmpSimpleName = proxyInterface.getSimpleName();
        String proxyInterfaceName = tmpSimpleName.substring(0,1).toLowerCase()
                +tmpSimpleName.substring(1);
        StringBuilder proxyInterfaceFieldString = new StringBuilder("private ");
        proxyInterfaceFieldString.append(proxyInterface.getName()).append(" ")
                .append(proxyInterfaceName).append(";");
        System.out.println(proxyInterfaceFieldString);
        CtField ctField = CtField.make(proxyInterfaceFieldString.toString(), proxyClazz);
        proxyClazz.addField(ctField);
        // 继承所有的公有方法，调用接口对应的方法
        CtMethod[] ctMethods = getMethodsWithoutObjectMethods(oldClass,pool);
        for(CtMethod method : ctMethods){
            CtMethod proxyMethod = CtNewMethod.copy(method, proxyClazz, null);
            for(Object attr: method.getMethodInfo().getAttributes()){
                if(attr.getClass().isAssignableFrom(AnnotationsAttribute.class)){
                    AnnotationsAttribute attribute = (AnnotationsAttribute)attr;
                    proxyMethod.getMethodInfo().addAttribute(attribute);
                }
            }
            String argsString = genArgsString(method);
            // 方法体
            StringBuilder body = new StringBuilder("{");
            if(method.getReturnType() == pool.get(void.class.getName())){
                body.append(proxyInterfaceName).append(".").append(method.getName()).append("(")
                        .append(argsString).append(");");
            }else{
                body.append("return ").append(proxyInterfaceName).append(".").append(method.getName()).append("(")
                        .append(argsString).append(");");
            }
            body.append("}");
            proxyMethod.setBody(body.toString());
            proxyClazz.addMethod(proxyMethod);
        }
        Class<?> newProxyClazz = proxyClazz.toClass();
        System.out.println(newProxyClazz.getName());
        return newProxyClazz;
    }

    private static String genArgsString(CtMethod oldMethod) throws NotFoundException {
        StringBuilder args = new StringBuilder("");
        for(int i=1; i <= oldMethod.getParameterTypes().length; i++) {
            args.append("$"+i).append(",");
        }
        if(args.length() > 0) {
            args.deleteCharAt(args.length() - 1);
        }
        return args.toString();
    }

    private static Class<?> generateProxyInterface(Object object) throws NotFoundException, CannotCompileException {
        // 1、获取所有公有方法 ,根据公有方法生成接口
        ClassPool pool = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()); //获得类池
        CtClass oldClass = pool.get(object.getClass().getName());
        CtMethod[] ctMethods = getMethodsWithoutObjectMethods(oldClass,pool);
        CtClass ct = pool.makeInterface(oldClass.getName()+"$ProxyInterface");
        //
        for(CtMethod method : ctMethods){
            //
            StringBuilder sb = new StringBuilder();
            sb.append("public ").append(method.getReturnType().getName()).append(" ")
                    .append(method.getName()).append("(");
            {
                int index = 0;
                for(CtClass ctClass : method.getParameterTypes()){
                    if(index > 0){
                        sb.append(",");
                    }
                    sb.append(ctClass.getName()).append(" args").append(index++);
                }
            }
            sb.append(") ");
            CtClass[] exceptionClasses = method.getExceptionTypes();
            if(exceptionClasses != null && exceptionClasses.length > 0){
                sb.append("throws ");
                int index = 0;
                for(CtClass exceptionClass : exceptionClasses){
                    if(index++ > 0){
                        sb.append(",");
                    }
                    sb.append(exceptionClass.getName());
                }
            }
            sb.append(";");

            System.out.println("zheli:"+sb.toString());
            CtMethod newMethod = CtMethod.make(sb.toString(),ct);
            ct.addMethod(newMethod);
        }
        Class<?> clz = ct.toClass();
        System.out.println(clz.getName());

        return clz;
    }

    private static CtMethod[] getMethodsWithoutObjectMethods(CtClass clz,ClassPool pool) throws NotFoundException {
        CtClass objectClass = pool.get(Object.class.getName());
        CtMethod[] ctMethods = clz.getMethods();

        List<CtMethod> methodList = new ArrayList();
        for(CtMethod method : ctMethods){
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if(method.getDeclaringClass() != objectClass){
                methodList.add(method);
            }
        }

        return methodList.toArray(new CtMethod[methodList.size()]);
    }
}
