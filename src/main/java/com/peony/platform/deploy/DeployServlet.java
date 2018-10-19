package com.peony.platform.deploy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.gm.GmSegment;
import com.peony.engine.framework.control.gm.GmService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by a on 2016/9/29.
 */
public class DeployServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(DeployServlet.class);
    private GmService gmService;
    private String gmJsonStr;

    public void init() throws ServletException{
//        gmService = BeanHelper.getServiceBean(GmService.class);
//        Map<String, GmSegment> gmSegmentMap = gmService.getGmSegments();
//        JSONObject jsonObject = new JSONObject();
//        // 加个排序
//        List<GmSegment> gmSegmentList = new ArrayList<>(gmSegmentMap.values());
//        gmSegmentList.sort(new Comparator<GmSegment>() {
//            @Override
//            public int compare(GmSegment o1, GmSegment o2) {
//                return o1.getId().compareTo(o2.getId());
//            }
//        });
//        //
//        for(GmSegment gmSegment : gmSegmentList){
//            JSONObject item = new JSONObject();
//            item.put("id",gmSegment.getId());
//            item.put("describe",gmSegment.getDescribe());
//            JSONObject type = new JSONObject();
//            int i=0;
//            int nameLength = gmSegment.getParamsName().length;
//            for(Class cls : gmSegment.getParamsType()){
//                String name = "param";
//                if(nameLength>i){
//                    name=gmSegment.getParamsName()[i];
//                }
//                type.put("param"+i,name+"("+cls.getSimpleName()+")");
//                i++;
//            }
//            item.put("type",type);
//            jsonObject.put(gmSegment.getId(),item);
//        }
//        gmJsonStr = jsonObject.toString();
//        log.info("init gmServlet finish");
    }

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

    private void doGm(HttpServletRequest req, HttpServletResponse resp) throws Throwable{
        String oper = req.getParameter("oper");
        if(oper==null){
            throw new MMException("oper==null");
        }
        if(oper.equals("begin")){
            resp.getWriter().write(gmJsonStr);
        }else if(oper.equals("deploySubmit")){
//            String gm = req.getParameter("gm");
//            System.out.println(new File("").toURL().toURI());
//            System.out.println(new File("").toPath());
////            new Directory
//            System.out.println(new File("").isDirectory());
            execShell("sh www/deploy/deploy.sh >");
        }else{
            throw new MMException("oper is error,oper="+oper);
        }
    }


    public static void execShell(String shpath){
        try {
//            String shpath="/home/felven/word2vec/demo-classes.sh";
            Process ps = Runtime.getRuntime().exec(shpath);
            ps.waitFor();

            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            String result = sb.toString();
            System.out.println(result);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
