package com.peony.common.tool.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类操作工具类
 *
 * @author huangyong
 * @since 1.0
 */
public class ClassUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);

    /**
     * void(V).
     */
    public static final char JVM_VOID = 'V';

    /**
     * boolean(Z).
     */
    public static final char JVM_BOOLEAN = 'Z';

    /**
     * byte(B).
     */
    public static final char JVM_BYTE = 'B';

    /**
     * char(C).
     */
    public static final char JVM_CHAR = 'C';

    /**
     * double(D).
     */
    public static final char JVM_DOUBLE = 'D';

    /**
     * float(F).
     */
    public static final char JVM_FLOAT = 'F';

    /**
     * int(I).
     */
    public static final char JVM_INT = 'I';

    /**
     * long(J).
     */
    public static final char JVM_LONG = 'J';

    /**
     * short(S).
     */
    public static final char JVM_SHORT = 'S';

    /**
     * MethodSignature 缓存
     */
    public static final Map<Method,String> methodSignatureCache = new ConcurrentHashMap<>();

    public static String getMethodSignature(Method method){
        if(method == null){
            return null;
        }
        String signature = methodSignatureCache.get(method);
        if(signature != null){
            return signature;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName());
        sb.append(method.getName());
        for(Class<?> ptype:method.getParameterTypes()){
            sb.append(ptype.getName());
        }
        signature = sb.toString();
        methodSignatureCache.put(method,signature);
        return signature;
    }

    /**
     * get method desc.
     * int do(int arg1) => "do(I)I"
     * void do(String arg1,boolean arg2) => "do(Ljava/lang/String;Z)V"
     *
     * @param m method.
     * @return desc.
     */
    public static String getDesc(final Method m)
    {
        StringBuilder ret = new StringBuilder(m.getName()).append('(');
        Class<?>[] parameterTypes = m.getParameterTypes();
        for(int i=0;i<parameterTypes.length;i++)
            ret.append(getDesc(parameterTypes[i]));
        ret.append(')').append(getDesc(m.getReturnType()));
        return ret.toString();
    }

    /**
     * get class desc.
     * boolean[].class => "[Z"
     * Object.class => "Ljava/lang/Object;"
     *
     * @param c class.
     * @return desc.
     */
    public static String getDesc(Class<?> c)
    {
        StringBuilder ret = new StringBuilder();

        while( c.isArray() )
        {
            ret.append('[');
            c = c.getComponentType();
        }

        if( c.isPrimitive() )
        {
            String t = c.getName();
            if( "void".equals(t) ) ret.append(JVM_VOID);
            else if( "boolean".equals(t) ) ret.append(JVM_BOOLEAN);
            else if( "byte".equals(t) ) ret.append(JVM_BYTE);
            else if( "char".equals(t) ) ret.append(JVM_CHAR);
            else if( "double".equals(t) ) ret.append(JVM_DOUBLE);
            else if( "float".equals(t) ) ret.append(JVM_FLOAT);
            else if( "int".equals(t) ) ret.append(JVM_INT);
            else if( "long".equals(t) ) ret.append(JVM_LONG);
            else if( "short".equals(t) ) ret.append(JVM_SHORT);
        }
        else
        {
            ret.append('L');
            ret.append(c.getName().replace('.', '/'));
            ret.append(';');
        }
        return ret.toString();
    }


    /**
     * 获取类加载器
     */
    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 获取类路径
     */
    public static String getClassPath() {
        String classpath = "";
        URL resource = getClassLoader().getResource("");
        if (resource != null) {
            classpath = resource.getPath();
        }
        return classpath;
    }

    /**
     * 加载类（将自动初始化）
     */
    public static Class<?> loadClass(String className) {
        return loadClass(className, true);
    }

    /**
     * 加载类
     */
    public static <T> Class<T> loadClass(String className, boolean isInitialized) {
        Class<T> cls;
        try {
            cls = (Class<T>)Class.forName(className, isInitialized, getClassLoader());
        } catch (ClassNotFoundException e) {
            logger.error("加载类出错！", e);
            throw new RuntimeException(e);
        }
        return cls;
    }

    /**
     * 是否为 int 类型（包括 Integer 类型）
     */
    public static boolean isInt(Class<?> type) {
        return type.equals(int.class) || type.equals(Integer.class);
    }

    /**
     * 是否为 long 类型（包括 Long 类型）
     */
    public static boolean isLong(Class<?> type) {
        return type.equals(long.class) || type.equals(Long.class);
    }

    /**
     * 是否为 double 类型（包括 Double 类型）
     */
    public static boolean isDouble(Class<?> type) {
        return type.equals(double.class) || type.equals(Double.class);
    }

    /**
     * 是否为 String 类型
     */
    public static boolean isString(Class<?> type) {
        return type.equals(String.class);
    }
}
