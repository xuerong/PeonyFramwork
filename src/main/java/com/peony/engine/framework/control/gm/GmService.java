package com.peony.engine.framework.control.gm;

import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * gm的服务类。
 * <p>
 * 在系统启动的时候进行GM方法的校验和预处理，缓存。在GM请求来时通过反射调用相应的GM方法。
 *
 * @author zhengyuzhen
 * @see GmSegment
 * @see Gm
 * @see GmAdmin
 * @see GmServlet
 * @see GmFilter
 * @since 1.0
 */
@Service(init = "init")
public class GmService {
    private static final Logger log = LoggerFactory.getLogger(GmService.class);
    /**
     * gm反射调用缓存
     */
    private Map<String,GmSegment> gmSegments;

    public void init(){
        gmSegments = new HashMap<>();
        // 取出gm的所有方法的参数
        Map<String,Method> gmMethods = ServiceHelper.getGmMethod();
        for(Map.Entry<String,Method> entry : gmMethods.entrySet()){
            Method method = entry.getValue();
            Class returnType = method.getReturnType();
            // 校验返回值
            if(returnType != Void.class && returnType!=Map.class && returnType!=String.class&& returnType!=void.class){
                throw new MMException("gm method returnType error,id="+entry.getKey()+",returnType="+returnType);
            }
            GmSegment gmSegment = new GmSegment();
            gmSegment.setReturnType(returnType);
            Class[] paramsTypes = method.getParameterTypes();
            // 校验参数：
            for(Class cls: paramsTypes){
                if(!isGmPermitType(cls)){
                    throw new MMException("gm param error, just permit primitive and String!");
                }
            }
            gmSegment.setParamsType(paramsTypes);
            gmSegment.setMethod(method);
            Object service = BeanHelper.getServiceBean(method.getDeclaringClass());
            gmSegment.setService(service);
            Gm gm = method.getAnnotation(Gm.class);
            gmSegment.setParamsName(gm.paramsName());
            gmSegment.setId(gm.id());
            gmSegment.setDescribe(gm.describe());
            gmSegments.put(gm.id(),gmSegment);
        }
    }

    public Map<String, GmSegment> getGmSegments() {
        return gmSegments;
    }

    /**
     * 处理gm，注意这里传过来的参数全是引用数据类型，不是基本类型
     *
     * @param id gm的id
     * @param params 参数值
     */
    public Object handle(String id,Object... params){
        GmSegment gmSegment = gmSegments.get(id);
        if(gmSegment == null){
            throw new MMException("gm is not exist , id = "+id);
        }
        // 校验参数
        Class[] clses = gmSegment.getParamsType();
        if(clses.length>params.length){
            throw new MMException("gm params error!need "+clses.length+" params"+",but get "+params.length);
        }
        int i=0;
        for(Class cls : clses){
            if(!castPrimitiveClass(cls).isAssignableFrom(params[i].getClass())){
                throw new MMException("gm params error, id = "+id);
            }
            i++;
        }
        // 调用
        try {
            log.info("dogm,request:[{}]  {}",id,params);
            Object result = gmSegment.getMethod().invoke(gmSegment.getService(), params);
            log.info("dogm,response:[{}]  {}",id,result);
            return result;
        }catch (Throwable e){
            if(e instanceof ToClientException){
                ToClientException toClientException = (ToClientException)e;
                return "exception,errcode:"+toClientException.getErrCode()+",errMsg:"+toClientException.getMessage();
            }
            throw new MMException(e);
        }
    }

    /**
     * 如果是原始类型，转换成封装类型
     *
     * @param cls
     */
    private Class castPrimitiveClass(Class cls){
        if(cls.isPrimitive()){
            if(cls == int.class) cls = Integer.class;
            else if(cls == long.class) cls = Long.class;
            else if(cls == float.class) cls = Float.class;
            else if(cls == double.class) cls = Double.class;
            else if(cls == char.class) cls = Character.class;
            else if(cls == byte.class) cls = Byte.class;
            else if(cls == boolean.class) cls = Boolean.class;
            else if(cls == short.class) cls = Short.class;
        }
        return cls;
    }

    /**
     * 校验参数，基本类型或者String
     *
     * @param cls
     * @return
     */
    private boolean isGmPermitType(Class cls){
        if(cls.isPrimitive()){
            return true;
        }
        if(cls == Integer.class) return true;
        else if(cls == Long.class) return true;
        else if(cls == Float.class) return true;
        else if(cls == Double.class) return true;
        else if(cls == Character.class) return true;
        else if(cls == Byte.class) return true;
        else if(cls == Boolean.class) return true;
        else if(cls == Short.class) return true;
        else if(cls == String.class) return true;
        return false;
    }
}
