package com.peony.cluster;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.common.utils.ArrayUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @author z84150192
 * @since 2020/9/27
 */
public class DubboHelper {
    public static Class<?> generateProxyInterface(Object object) throws NotFoundException, CannotCompileException {
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
