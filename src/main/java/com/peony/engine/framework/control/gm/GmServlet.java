package com.peony.engine.framework.control.gm;

import com.alibaba.fastjson.JSON;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * 接受GM请求的Servlet类。
 *
 * @author zhengyuzhen
 * @see GmSegment
 * @see Gm
 * @see GmAdmin
 * @see GmService
 * @see GmFilter
 * @since 1.0
 */
public class GmServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(GmServlet.class);
    private GmService gmService;
    private String gmJsonStr;

    public void init() throws ServletException{
        gmService = BeanHelper.getServiceBean(GmService.class);
        Map<String, GmSegment> gmSegmentMap = gmService.getGmSegments();
        JSONObject jsonObject = new JSONObject();
        // 加个排序
        List<GmSegment> gmSegmentList = new ArrayList<>(gmSegmentMap.values());
        gmSegmentList.sort(new Comparator<GmSegment>() {
            @Override
            public int compare(GmSegment o1, GmSegment o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        // 缓存GM的json值
        for(GmSegment gmSegment : gmSegmentList){
            JSONObject item = new JSONObject();
            item.put("id",gmSegment.getId());
            item.put("describe",gmSegment.getDescribe());
            JSONObject type = new JSONObject();
            int i=0;
            int nameLength = gmSegment.getParamsName().length;
            for(Class cls : gmSegment.getParamsType()){
                String name = "param";
                if(nameLength>i){
                    name=gmSegment.getParamsName()[i];
                }
                type.put("param"+i,name+"("+cls.getSimpleName()+")");
                i++;
            }
            item.put("type",type);
            jsonObject.put(gmSegment.getId(),item);
        }
        gmJsonStr = jsonObject.toString();
        log.info("init gmServlet finish");
    }

    /**
     * gm http消息入口
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html;charset=utf-8");
            doGm(req, resp);
        }catch (Throwable e){
            resp.getWriter().write("error:"+e.getMessage());
        }
    }

    /**
     * gm消息的处理
     *
     * @param req
     * @param resp
     * @throws Throwable
     */
    private void doGm(HttpServletRequest req, HttpServletResponse resp) throws Throwable{
        String oper = req.getParameter("oper");
        if(oper==null){
            throw new MMException("oper==null");
        }
        if(oper.equals("begin")){ // 获取gm界面信息
            resp.getWriter().write(gmJsonStr);
        }else if(oper.equals("gmSubmit")){ // gm访问消息
            String gm = req.getParameter("gm"); // gmId
            GmSegment gmSegment = gmService.getGmSegments().get(gm);
            if(gmSegment == null){
                throw new MMException("gmSegment is not exist,gm = "+gm);
            }
            // 参数转换
            Class[] paramTypes = gmSegment.getParamsType();
            Object[] param = new Object[paramTypes.length];
            int i=0;
            for(Class paramType:paramTypes){
                String p = req.getParameter("param"+i);
                if(p==null){
                    throw new MMException("param error, param"+i+" is not exist");
                }
                param[i] = stringToObject(p,paramType);
                i++;
            }
            Object ret = gmService.handle(gm,param);
            if(ret== null || ret.getClass() == Void.class || ret.getClass()==void.class){
                resp.getWriter().write("void");
            }else if(ret.getClass()==Map.class){
                resp.getWriter().write(mapToString((Map)ret));
            }else if(ret.getClass()==String.class){
                resp.getWriter().write(ret.toString());
            }else{
                resp.getWriter().write(ret.toString());
            }
        }else{
            throw new MMException("oper is error,oper="+oper);
        }
    }

    /**
     * String数据转化为java对象：基本类型或String
     * @param str
     * @param cls
     * @return
     */
    public Object stringToObject(String str,Class cls){
        if(str.length()==0 && cls != String.class){
            if(cls == boolean.class || cls == Boolean.class){
                str="false";
            }else{
                str="0";
            }
        }
        if(cls == int.class || cls == Integer.class){
            return Integer.parseInt(str);
        }else if(cls == long.class || cls == Long.class){
            return Long.parseLong(str);
        }else if(cls == float.class || cls == Float.class){
            return Float.parseFloat(str);
        }else if(cls == double.class || cls == Double.class){
            return Double.parseDouble(str);
        }else if(cls == char.class || cls == Character.class){
            return str.charAt(0);
        }else if(cls == byte.class || cls == Byte.class){
            return Byte.parseByte(str);
        }else if(cls == boolean.class || cls == Boolean.class){
            return Boolean.parseBoolean(str);
        }else if(cls == short.class || cls == Short.class){
            return Short.parseShort(str);
        }else if(cls == String.class){
            return str;
        }

        return str;
    }

    public String mapToString(Map map){
        return JSON.toJSONString(map);
    }
}
