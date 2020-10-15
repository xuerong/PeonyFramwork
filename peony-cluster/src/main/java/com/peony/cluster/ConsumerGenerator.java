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

/**
 * @author z84150192
 * @since 2020/9/27
 */
public class ConsumerGenerator {
    public static Object generateConsumer(Class<?> serviceClass, Object object) throws NotFoundException, CannotCompileException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        // 生成代理接口
        Class<?> proxyInterface = DubboHelper.generateProxyInterface(serviceClass);
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
        CtMethod[] ctMethods = DubboHelper.getMethodsWithoutObjectMethods(oldClass,pool);
        for(CtMethod method : ctMethods){
            CtMethod proxyMethod = CtNewMethod.copy(method, proxyClazz, null);
            for(Object attr: method.getMethodInfo2().getAttributes()){
                if(attr.getClass().isAssignableFrom(AnnotationsAttribute.class)){
                    AnnotationsAttribute attribute = (AnnotationsAttribute)attr;
                    proxyMethod.getMethodInfo2().addAttribute(attribute);
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

    private static String genArgsString(CtMethod oldMethod) throws NotFoundException{
        StringBuilder args = new StringBuilder("");
        for(int i=1; i <= oldMethod.getParameterTypes().length; i++) {
            args.append("$"+i).append(",");
        }
        if(args.length() > 0) {
            args.deleteCharAt(args.length() - 1);
        }
        return args.toString();
    }
}
