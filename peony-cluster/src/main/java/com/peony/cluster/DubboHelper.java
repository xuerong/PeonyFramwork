package com.peony.cluster;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.common.utils.ArrayUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author z84150192
 * @since 2020/9/27
 */
public class DubboHelper {
    public static Class<?> generateProxyInterface(Class<?> serviceClass) throws NotFoundException, CannotCompileException {
        // 1、获取所有公有方法 ,根据公有方法生成接口
        ClassPool pool = ClassGenerator.getClassPool(Thread.currentThread().getContextClassLoader()); //获得类池
        CtClass oldClass = pool.get(serviceClass.getName());
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
            if(ArrayUtils.isNotEmpty(exceptionClasses)){
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

        try{
            CodeSource codeSource = clz.getProtectionDomain().getCodeSource();
            CodeSource codeSourceOri = serviceClass.getProtectionDomain().getCodeSource();
            // 将动态生成的class的Location设置为原始class的，这里是为了屏蔽dubbo的报错
            // 【at org.apache.dubbo.common.Version.getVersion(Version.java:182)】
            Field field = CodeSource.class.getDeclaredField("location");
            field.setAccessible(true);
            field.set(codeSource,codeSourceOri.getLocation());
        }catch (Exception e){
            e.printStackTrace();
        }

        return clz;
    }

    public static CtMethod[] getMethodsWithoutObjectMethods(CtClass clz,ClassPool pool) throws NotFoundException {
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
