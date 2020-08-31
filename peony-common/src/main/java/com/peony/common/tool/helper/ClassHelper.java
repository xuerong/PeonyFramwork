package com.peony.common.tool.helper;

import com.peony.common.exception.MMException;
import com.peony.common.tool.AnnotationClassTemplate;
import com.peony.common.tool.ClassTemplate;
import com.peony.common.tool.EndWithClassTemplate;
import com.peony.common.tool.SupperClassTemplate;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * 根据条件获取相关类
 *
 * @author huangyong
 * @since 1.0
 */
public class ClassHelper {

    private static void checkPacket(String... packets){
        if(packets == null || packets.length == 0){
            throw new MMException("packets is empty");
        }
        if(packets.length == 1){
            return;
        }
        for(int i=0;i<packets.length;i++){
            for(int j=i+1;j<packets.length;j++){
                String packet1 = packets[i];
                String packet2 = packets[j];
                if(packet1.startsWith(packet2) || packet2.startsWith(packet1)){
                    throw new MMException("basePackage and appPackage duplicate,don't contain each other" +
                            "packet1 = "+packet1+",packet2="+packet2);
                }
            }
        }
    }

    public static boolean containPacket(String packetName,String... packets){
        checkPacket(packets);
        for(String packet : packets){
            if(packetName.startsWith(packet)){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取基础包名中的所有类
     */
    public static List<Class<?>> getClassList(String... packets) {
        checkPacket(packets);
        List<Class<?>> result = new ArrayList<>();
        for(String packet : packets){
            result.addAll(getClassList(packet));
        }
        return  result;
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
    public static List<Class<?>> getClassListBySuper(Class<?> superClass,String... packets) {
        checkPacket(packets);
        List<Class<?>> result = new ArrayList<>();
        for(String packet : packets){
            List<Class<?>> clses=new SupperClassTemplate(packet, superClass) {
                @Override
                public boolean checkAddClass(Class<?> cls) {
                    return superClass.isAssignableFrom(cls) && !superClass.equals(cls);
                }
            }.getClassList();

            result.addAll(clses);
        }
        return  result;
    }

    /**
     * 获取基础包名中指定注解的相关类
     */
    public static List<Class<?>> getClassListByAnnotation(Class<? extends Annotation> annotationClass,String... packets) {
        checkPacket(packets);
        List<Class<?>> result = new ArrayList<>();
        for(String packet : packets){
            List<Class<?>> clses= new AnnotationClassTemplate(packet, annotationClass) {
                @Override
                public boolean checkAddClass(Class<?> cls) {
                    return cls.isAnnotationPresent(annotationClass);
                }
            }.getClassList();

            result.addAll(clses);
        }
        return  result;
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
