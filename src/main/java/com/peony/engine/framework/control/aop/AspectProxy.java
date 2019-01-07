package com.peony.engine.framework.control.aop;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.aop.annotation.Aspect;
import com.peony.engine.framework.control.aop.annotation.AspectMark;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用于实现AOP系统的切面类父类。
 *
 * @author zhengyuzhen
 * @see Proxy
 * @see Aspect
 * @see AopHelper
 * @since 1.0
 */
public abstract class AspectProxy implements Proxy {
    // 要代理的对象-标识（mark）列表
    private List<String> markList=null;
    // 要代理的对象-注解（annotation）列表
    private List<Class<? extends Annotation>> annotationList=null;
    // 要代理的对象-包（pkg）列表
    private List<String> pkgList=null;
    // 该代理对应的所有方法（Service原生类的方法），执行检查用
    private Set<Method> executeMethod=null;
    // 是否对切点类所有的方法执行代理
    private boolean isExecuteAllMethod=false;


    public AspectProxy(){
        // 初始化要代理的对象
        Aspect aspect=this.getClass().getAnnotation(Aspect.class);
        if(aspect!=null){
            markList=AopUtil.getMarkList(aspect);
            annotationList=AopUtil.getAnnotationClassList(aspect);
            pkgList=AopUtil.getPkgList(aspect);
        }
    }

    /**
     * 该方法是否满足执行的条件。
     * <p>
     * 通过对切点方法的缓存来提高判断效率。
     *
     * @param method 被代理的方法
     * @return
     */
    @Override
    public boolean executeMethod(Method method) {
        if(isExecuteAllMethod){
            return  true;
        }
        Method oldMethod = ServiceHelper.getOverrideMethod().get(method);
        if(executeMethod!=null && executeMethod.contains(oldMethod==null?method:oldMethod)){
            return true;
        }
        return false;
    }

    /**
     * 这个函数在该代理对象被创建时调用，持有该对象所代理的目标类，
     * 并用来判断是否全类代理以及要代理的方法，这里判断出
     * 这样将方法的赛选在初始化的时候完成，减少执行期间的计算
     *
     * @param targetClass 目标类
     */
    @Override
    public void setTargetClass(Class<?> targetClass) {
        if(AopUtil.isExecuteByPkg(targetClass, pkgList)){
            isExecuteAllMethod=true;
            return;
        }

        if(AopUtil.isExecuteAllByAnnotation(targetClass,annotationList)){
            isExecuteAllMethod=true;
            return;
        }

        if(AopUtil.isExecuteAllByMark(targetClass,markList)){
            isExecuteAllMethod=true;
            return;
        }
        // 如果上述类切面识别没有成功，那么就是方法切面，需要通过annotation和mark识别
        executeMethod=new HashSet<>();
        Method[] methods = targetClass.getMethods();

//        nextMethod:
        for(Method method: methods){
            doIt(method);
        }
    }
    private boolean doIt(Method method){
        //annotation既可能是方法，也可能是类，判断方法
        if(annotationList!=null) {
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotationList.contains(annotation.annotationType())) {
                    executeMethod.add(method);
                    return true;
                }
            }
        }
        // mask既可能是方法，也可能是类，判断方法
        if(markList!=null){
            AspectMark aspectMark = AopUtil.methodAnnotationPresent(method);
            if(aspectMark !=null){
                String[] marks = aspectMark.mark();
                for (String mark:marks) {
                    if(markList.contains(mark)){
                        if(!executeMethod.contains(method)) {
                            executeMethod.add(method);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
