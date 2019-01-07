package com.peony.engine.framework.tool.helper;

import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.tool.AnnotationClassTemplate;
import com.peony.engine.framework.tool.ClassTemplate;
import com.peony.engine.framework.tool.EndWithClassTemplate;
import com.peony.engine.framework.tool.SupperClassTemplate;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * 根据条件获取相关类
 *
 * @author huangyong
 * @since 1.0
 */
public class ClassHelper {

    /**
     * 获取基础包名
     */
    public static final String basePackage = "com.peony";
    public static final String appPackage;

    static {
        // 系统包
        // 用户定义的包
        appPackage = Server.getEngineConfigure().getString("appPackage");
        // 要校验两个包是否重复
        if(basePackage.startsWith(appPackage) || appPackage.startsWith(basePackage)){
            throw new MMException("basePackage and appPackage duplicate,don't contain each other" +
                    "basePacket = "+basePackage+",appPacket="+appPackage);
        }
    }

    public static boolean containPacket(String packetName){
        if(packetName.startsWith(basePackage) || packetName.startsWith(appPackage)){
            return true;
        }
        return false;
    }

    /**
     * 获取基础包名中的所有类
     */
    public static  List<Class<?>> getClassList() {
        List<Class<?>> result=new ClassTemplate(basePackage) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                String className = cls.getName();
                String pkgName = className.substring(0, className.lastIndexOf("."));
                return pkgName.startsWith(packageName);
            }
        }.getClassList();
        result.addAll(new ClassTemplate(appPackage) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                String className = cls.getName();
                String pkgName = className.substring(0, className.lastIndexOf("."));
                return pkgName.startsWith(appPackage);
            }
        }.getClassList());
        return  result;
//        return new ClassTemplate(basePackage) {
//            @Override
//            public boolean checkAddClass(Class<?> cls) {
//                String className = cls.getName();
//                String pkgName = className.substring(0, className.lastIndexOf("."));
//                return pkgName.startsWith(packageName);
//            }
//        }.getClassList();
    }

    /**
     * 根据包名获取类
     * @return
     */
    public static List<Class<?>> getClassList(String packet) {
        List<Class<?>> result=new ClassTemplate(packet) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                String className = cls.getName();
                String pkgName = className.substring(0, className.lastIndexOf("."));
                return pkgName.startsWith(packet);
            }
        }.getClassList();
        return result;
    }

    /**
     * 获取基础包名中指定父类或接口的相关类
     * @param superClass
     * @return
     */
    public static List<Class<?>> getClassListBySuper(Class<?> superClass) {
        List<Class<?>> result=new SupperClassTemplate(basePackage, superClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
            }
        }.getClassList();
        result.addAll(new SupperClassTemplate(appPackage, superClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
            }
        }.getClassList());
        return result;
//        return new SupperClassTemplate(basePackage, superClass) {
//            @Override
//            public boolean checkAddClass(Class<?> cls) {
//                return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
//            }
//        }.getClassList();
    }

    /**
     * 获取基础包名中指定注解的相关类
     */
    public static List<Class<?>> getClassListByAnnotation(Class<? extends Annotation> annotationClass) {
        List<Class<?>> result=new AnnotationClassTemplate(basePackage, annotationClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return cls.isAnnotationPresent(annotationClass);
            }
        }.getClassList();
        result.addAll(new AnnotationClassTemplate(appPackage, annotationClass) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return cls.isAnnotationPresent(annotationClass);
            }
        }.getClassList());
        return result;
//        return new AnnotationClassTemplate(basePackage, annotationClass) {
//            @Override
//            public boolean checkAddClass(Class<?> cls) {
//                return cls.isAnnotationPresent(annotationClass);
//            }
//        }.getClassList();
    }
    /**
     * 获取以某个字符串结尾的类
     */
    public static List<Class<?>> getClassListEndWith(String packet,String endWith) {
        List<Class<?>> result=new EndWithClassTemplate(packet, endWith) {
            @Override
            public boolean checkAddClass(Class<?> cls) {
                return cls.getName().endsWith(endWithString);
            }
        }.getClassList();
        return result;
    }
}
