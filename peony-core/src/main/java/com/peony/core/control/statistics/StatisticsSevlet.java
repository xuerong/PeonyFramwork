package com.peony.core.control.statistics;

import com.alibaba.fastjson.JSON;
import com.peony.common.exception.MMException;
import com.peony.core.control.BeanHelper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by a on 2017/12/26.
 */
public class StatisticsSevlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(StatisticsSevlet.class);
    private StatisticsService statisticsService;
    private String tabJson;

    @Override
    public void init() throws ServletException{
        statisticsService = BeanHelper.getServiceBean(StatisticsService.class);
        Map<String,String> map = statisticsService.getTabMap();
        JSONObject jsonObject = new JSONObject();
        // 加个排序
//        tabList.sort();
        //
        //
        for(Map.Entry<String,String> entry : map.entrySet()){
            jsonObject.put(entry.getKey(),entry.getValue());
        }
        tabJson = jsonObject.toString();
        log.info("init gmServlet finish");
    }
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html;charset=utf-8");
            doStaticstics(req, resp);
        }catch (Throwable e){
            resp.getWriter().write("error:"+e.getMessage());
        }
    }

    private void doStaticstics(HttpServletRequest req, HttpServletResponse resp) throws Throwable{
        String oper = req.getParameter("oper");
        if(oper==null){
            throw new MMException("oper==null");
        }
        if(oper.equals("begin")){
            resp.getWriter().write(tabJson);
        }else if(oper.equals("tabSubmit")){
            String value = req.getParameter("value");
//            log.info(value);

            StatisticsData statisticsData = statisticsService.getData(value);
            if(statisticsData == null){
                resp.getWriter().write("no data");
                return;
            }
            JSONObject item = new JSONObject();

            JSONArray headArray = new JSONArray();
            for(String head :statisticsData.getHeads()){
                headArray.add(head);
            }
            item.put("heads",headArray);

            JSONArray datasArray = new JSONArray();
            for(List<String> datas :statisticsData.getDatas()){
                JSONArray dataArray = new JSONArray();
                for(String data : datas) {
                    dataArray.add(data);
                }
                datasArray.add(dataArray);
            }
            item.put("datas",datasArray);

//            log.info(item.toString());
            resp.getWriter().write(item.toString());
//            String gm = req.getParameter("gm");
//            GmSegment gmSegment = gmService.getGmSegments().get(gm);
//            if(gmSegment == null){
//                throw new MMException("gmSegment is not exist,gm = "+gm);
//            }
//            // 参数转换
//            Class[] paramTypes = gmSegment.getParamsType();
//            Object[] param = new Object[paramTypes.length];
//            int i=0;
//            for(Class paramType:paramTypes){
//                String p = req.getParameter("param"+i);
//                if(p==null){
//                    throw new MMException("param error, param"+i+" is not exist");
//                }
//                param[i] = stringToObject(p,paramType);
//                i++;
//            }
//            Object ret = gmService.handle(gm,param);
//            if(ret== null || ret.getClass() == Void.class || ret.getClass()==void.class){
//                resp.getWriter().write("void");
//            }else if(ret.getClass()==Map.class){
//                resp.getWriter().write(mapToString((Map)ret));
//            }else if(ret.getClass()==String.class){
//                resp.getWriter().write(ret.toString());
//            }else{
//                resp.getWriter().write(ret.toString());
//            }
        }else{
            throw new MMException("oper is error,oper="+oper);
        }
    }

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
