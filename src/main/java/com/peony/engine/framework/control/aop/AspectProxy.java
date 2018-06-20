package com.peony.engine.framework.control.aop;

import com.peony.engine.framework.control.aop.annotation.Aspect;
import com.peony.engine.framework.control.aop.annotation.AspectMark;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2015/11/17.
 */
public abstract class AspectProxy implements Proxy {
    // 要代理的对象
    private List<String> markList=null;
    private List<Class<? extends Annotation>> annotationList=null;
    private List<String> pkgList=null;
    // 执行检查，因为代理类未必执行当前执行的方法
    private List<Method> executeMethod=null;
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

    @Override
    public boolean executeMethod(Method method) {
        if(isExecuteAllMethod){
            return  true;
        }
        if(executeMethod!=null && executeMethod.contains(method)){
            return true;
        }
        return false;
    }

    // 这个函数在该代理对象被创建时调用，持有该对象所代理的目标类，
    // 并用来判断是否全类代理以及要代理的方法，这里判断出
    // 这样将方法的赛选在初始化的时候完成，减少执行期间的计算
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
        executeMethod=new LinkedList<>();
        Method[] methods = targetClass.getMethods();

        nextMethod:
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
