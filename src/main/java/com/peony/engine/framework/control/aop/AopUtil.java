package com.peony.engine.framework.control.aop;

import com.peony.engine.framework.control.aop.annotation.Aspect;
import com.peony.engine.framework.control.aop.annotation.AspectMark;
import com.peony.engine.framework.tool.helper.ClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2015/11/18.
 */
public class AopUtil {
    private static final Logger log = LoggerFactory.getLogger(AopUtil.class);
    protected static List<String> getMarkList(Aspect aspect){
        List<String> markList=null;
        String[] masks = aspect.mark();
        if(masks.length>0) {
            markList = new ArrayList<>();
            for (String mask : masks) {
                markList.add(mask);
            }
        }
        return markList;
    }
    protected static List<Class<? extends Annotation>> getAnnotationClassList(Aspect aspect){
        List<Class<? extends Annotation>> annotationList=null;
        Class<? extends Annotation>[] annotations=aspect.annotation();
        if(annotations.length>0) {
            annotationList = new ArrayList<Class<? extends Annotation>>();
            for (Class<? extends Annotation> annotation : annotations) {
                annotationList.add(annotation);
            }
        }
        return  annotationList;
    }
    protected static List<String> getPkgList(Aspect aspect){
        List<String> pkgList=null;
        String[] pkgs=aspect.pkg();
        if(pkgs.length>0) {
            pkgList = new ArrayList<>();
            for (String pkg : pkgs) {
                pkgList.add(pkg);
            }
        }
        return  pkgList;
    }
    /**
     * 判断是否属于@param aspect 的target
     * */
    protected static boolean isExecuteClass(Class<?> targetClass,Aspect aspect){
        return isExecuteByPkg(targetClass,aspect) || isExecuteByAnnotation(targetClass,aspect) ||
                isExecuteByMark(targetClass,aspect);
    }
    protected static boolean isExecuteByPkg(Class<?> targetClass,Aspect aspect){
        return isExecuteByPkg(targetClass,getPkgList(aspect));
    }
    protected static boolean isExecuteByAnnotation(Class<?> targetClass,Aspect aspect){
        List<Class<? extends Annotation>> annotationList=getAnnotationClassList(aspect);

        boolean result = isExecuteAllByAnnotation(targetClass,annotationList) || isExecutePartByAnnotation(targetClass,annotationList);

        return result;
    }
    protected static boolean isExecuteByMark(Class<?> targetClass,Aspect aspect){
        List<String> markList=getMarkList(aspect);
        return isExecuteAllByMark(targetClass,markList) || isExecutePartByMark(targetClass,markList);
    }
    protected static boolean isExecuteByPkg(Class<?> targetClass, List<String> pkgList){
        if(pkgList!=null) {
            String pkgName = targetClass.getPackage().getName();
            for(String pkg : pkgList){
                if(pkgName.startsWith(pkg)){
                    return true;
                }
            }
        }
        return false;
    }

    protected static  boolean isExecuteAllByAnnotation(Class<?> targetClass,List<Class<? extends Annotation>> annotationList){
        if(annotationList!=null){
            // annotation既可能是方法，也可能是类，先判断类
            Annotation[] annotations = targetClass.getAnnotations();
            for(Annotation annotation : annotations){
               // log.info("test:annotationClass:"+annotationList.get(0)+",annotation:"+annotation.annotationType());
                if(annotationList.contains(annotation.annotationType())){
                    return true;
                }
            }
        }
        return false;
    }
    protected static boolean isExecutePartByAnnotation(Class<?> targetClass,List<Class<? extends Annotation>> annotationList) {
        if (annotationList != null) {
            Method[] methods = targetClass.getMethods();
            for (Method method : methods) {
                //annotation既可能是方法，也可能是类，判断方法
                Annotation[] annotations = method.getAnnotations();
                for (Annotation annotation : annotations) {
                    if (annotationList.contains(annotation.annotationType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    protected static boolean isExecuteAllByMark(Class<?> targetClass,List<String> markList){
        if(markList!=null){
            // mask既可能是方法，也可能是类，先判断类
            if(targetClass.isAnnotationPresent(AspectMark.class)){
                AspectMark aspectMask = targetClass.getAnnotation(AspectMark.class);
                if(isExecuteByMark(markList,aspectMask)){
                    return true;
                }
            }
        }
        return false;
    }
    protected static boolean isExecutePartByMark(Class<?> targetClass,List<String> markList){
        if (markList != null) {
            Method[] methods = targetClass.getMethods();
            for (Method method : methods) {
                //annotation既可能是方法，也可能是类，判断方法
                AspectMark aspectMark = methodAnnotationPresent(method);
                if(aspectMark != null){
                    if(isExecuteByMark(markList,aspectMark)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    // 用这个判断一个方法是否属于一个AspectMark，或考虑其父类的对应方法是否属于AspectMark，并返回相应的AspectMark
    public static AspectMark methodAnnotationPresent(Method method){
        if(method.isAnnotationPresent(AspectMark.class)){
            return method.getAnnotation(AspectMark.class);
        }
        // 其父类的方法
        Class<?> cls = method.getDeclaringClass();;
        while(true) {
            cls = cls.getSuperclass();
            if(cls == null){
                return null;
            }
            if (!ClassHelper.containPacket(cls.getPackage().getName())) {
                return null;
            }
            try {
                Method cM = cls.getMethod(method.getName(), method.getParameterTypes());
                if(cM.isAnnotationPresent(AspectMark.class)){
                    return cM.getAnnotation(AspectMark.class);
                }
                continue;
            }catch (NoSuchMethodException e){
                continue;
            }
        }
    }
    private static boolean isExecuteByMark(List<String> markList,AspectMark aspectMask){
        String[] marks = aspectMask.mark();
        for (String mark:marks) {
            if(markList.contains(mark)){
                return true;
            }
        }
        return false;
    }
}
