package com.peony.demo;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.junit.Test;

/**
 * @Author: zhengyuzhen
 * @Date: 2019/4/26 3:31 PM
 */
public class TestGenService {
    @Test
    public void aaa() throws Exception{

        genService(ClassPool.getDefault(),TestGenService.class,null);
    }

    private static Class<?> genService(ClassPool pool, Class<?> serviceClass, Class<?> newServiceClass) throws Exception {


        // 只处理 当前类定义的 public 的方法

//        CtClass ctClazz = pool.get(serviceClass.getName());
//        CtClass cls = pool.makeClass(ctClazz.getName() + "$Proxy", ctClazz);
//
//        // 改写remote方法, 使用远程调用
//        for(CtMethod ctMethod:ctClazz.getMethods()) {
//            // 重写该方法
//            StringBuilder sb = new StringBuilder("public "+ctMethod.getReturnType().getName() + " " + ctMethod.getName() + "(");
//            CtClass[] paramClasses = ctMethod.getParameterTypes();
//            int i = 0;
//            StringBuilder paramsStr = new StringBuilder();
//            for (CtClass paramClass : paramClasses) {
//                if (i > 0) {
//                    sb.append(",");
//                    paramsStr.append(",");
//                }
//                sb.append(paramClass.getName() + " p" + i);
//                // 这个地方需要进行一步强制转换,基本类型不能编译成Object类型
//                paramsStr.append(parseBaseTypeStrToObjectTypeStr(paramClass.getName(), "p" + i));
//                i++;
//            }
//            sb.append(") {");
//            String paramStr = "null";
//            if (i > 0) {
//                sb.append("Object[] param = new Object[]{" + paramsStr + "};");
//                paramStr = "param";
//            }
//            // --------------这个地方要进行强制转换
//            sb.append("com.peony.core.control.netEvent.remote.RemoteCallService remoteCallService = (com.peony.core.control.netEvent.remote.RemoteCallService)com.peony.core.control.BeanHelper.getServiceBean(com.peony.core.control.netEvent.remote.RemoteCallService.class);");
//            String invokeStr = "remoteCallService.remoteCallMainServerSyn(" + serviceClass.getName() + ".class,\"" + ctMethod.getName() + "\"," + paramStr + ");";
//            if (ctMethod.getReturnType().getName().toLowerCase().equals("void")) {
//                sb.append(invokeStr);
//            } else {
//                sb.append("Object object = " + invokeStr);
//                sb.append("return " + parseBaseTypeStrToObjectTypeStr(ctMethod.getReturnType().getName()));
//            }
//            sb.append("}");
////            log.info("==============================================\n"+sb.toString());
//            System.out.println(sb);
//            CtMethod method = CtMethod.make(sb.toString(), cls);
//            cls.addMethod(method);
//        }
//        newServiceClass = cls.toClass();
//        return newServiceClass;

        return null;
    }



    public static String parseBaseTypeStrToObjectTypeStr(String typeStr) {
        if (typeStr.equals("byte")) {
            return "object==null?0:((Byte)object).byteValue();";
        } else if (typeStr.equals("short")) {
            return "object==null?0:((Short)object).shortValue();";//"Short";
        } else if (typeStr.equals("long")) {
            return "object==null?0:((Long)object).longValue();";//"Long";
        } else if (typeStr.equals("int")) {
            return "object==null?0:((Integer)object).intValue();";//"Integer";
        } else if (typeStr.equals("float")) {
            return "object==null?0:((Float)object).floatValue();";//"Float";
        } else if (typeStr.equals("double")) {
            return "object==null?0:((Double)object).doubleValue();";//"Double";
        } else if (typeStr.equals("char")) {
            return "object==null?0:((Character)object).charValue();";//"Character";
        } else if (typeStr.equals("boolean")) {
            return "object==null?false:((Boolean)object).booleanValue();";//"Boolean";
        }
        return "(" + typeStr + ")object;";
    }

    public static String parseBaseTypeStrToObjectTypeStr(String typeStr, String paramStr) {
        if (typeStr.equals("byte")) {
            return "new Byte(" + paramStr + ")";
        } else if (typeStr.equals("short")) {
            return "new Short(" + paramStr + ")";
        } else if (typeStr.equals("long")) {
            return "new Long(" + paramStr + ")";
        } else if (typeStr.equals("int")) {
            return "new Integer(" + paramStr + ")";
        } else if (typeStr.equals("float")) {
            return "new Float(" + paramStr + ")";
        } else if (typeStr.equals("double")) {
            return "new Double(" + paramStr + ")";
        } else if (typeStr.equals("char")) {
            return "new Character(" + paramStr + ")";
        } else if (typeStr.equals("boolean")) {
            return "new Boolean(" + paramStr + ")";
        }
        return paramStr;
    }
}
