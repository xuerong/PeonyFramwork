package com.peony.platform.deploy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.gm.GmService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by a on 2016/9/29.
 */
public class DeployServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(DeployServlet.class);
    private GmService gmService;
    private String gmJsonStr;
    private DeployService deployService;

    public void init() throws ServletException{
        deployService = BeanHelper.getServiceBean(DeployService.class);
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
            JSONObject ret = doGm(req, resp);
            resp.getWriter().write(ret.toJSONString());
//            resp.getWriter().flush();
        }catch (Throwable e){
            e.printStackTrace();
            if(e instanceof ToClientException){
                JSONObject ret = new JSONObject();
                ret.put("exception",((ToClientException) e).getMessage());
                resp.getWriter().write(ret.toJSONString());
                return;
            }
            JSONObject ret = new JSONObject();
            ret.put("exception",e.getMessage());
            resp.getWriter().write(ret.toJSONString());
            return;
//            resp.getWriter().flush();
        }
    }

    private JSONObject doGm(HttpServletRequest req, HttpServletResponse resp) throws Throwable{
        String oper = req.getParameter("oper");
        if(oper==null){
            throw new MMException("oper==null");
        }

        JSONObject ret  = new JSONObject();
        switch (oper){
            case "deploySubmit":break;
            case "getDeployProjects":
            {
                ret = deployService.getDeployProject();
            }
            break;
            case "setDeployProject":
            {
                String projectId = req.getParameter("projectId");
                String name = req.getParameter("name");
                String defaultSource = req.getParameter("defaultSource");
                String sourceParam = req.getParameter("sourceParam");
                ret = deployService.setDeployProject(projectId, name, defaultSource, sourceParam);
            }
            break;
            case "addDeployProject":
                {
                    String projectId = req.getParameter("projectId");
                    String name = req.getParameter("name");
                    String defaultSource = req.getParameter("defaultSource");
                    String sourceParam = req.getParameter("sourceParam");
                    ret = deployService.addDeployProject(projectId, name, defaultSource, sourceParam);
                }
                break;
            case "delDeployProject":
            {
                String projectId = req.getParameter("projectId");
                ret = deployService.delDeployProject(projectId);
            }
            break;
            case "getCodeOriginList":
            {
                String projectId = req.getParameter("projectId");
                ret = deployService.getCodeOriginList(projectId);
            }
            break;
            case "addCodeOrigin":
            {
                String projectId = req.getParameter("projectId");
                String id = req.getParameter("id");
                String name = req.getParameter("name");
                String type = req.getParameter("type");
                JSONObject params = new JSONObject();
                switch (Integer.parseInt(type)){
                    case 1:
                        String localPath = req.getParameter("localPath");
                        params.put("localPath",localPath);
                        break;
                    case 2:
                        String gitPath = req.getParameter("gitPath");
                        String gitBranch = req.getParameter("gitBranch");
                        String gitName = req.getParameter("gitName");
                        String gitPassword = req.getParameter("gitPassword");
                        params.put("gitPath",gitPath);
                        params.put("gitBranch",gitBranch);
                        params.put("gitName",gitName);
                        params.put("gitPassword",gitPassword);
                        break;
                    case 3:
                        String svnPath = req.getParameter("svnPath");
                        params.put("svnPath",svnPath);
                        break;
                }
                ret = deployService.addCodeOrigin(projectId, Integer.parseInt(id), name, Integer.parseInt(type), params.toJSONString());
            }
            break;
            case "delCodeOrigin":
            {
                String projectId = req.getParameter("projectId");
                String id = req.getParameter("id");
                ret = deployService.delCodeOrigin(projectId, Integer.parseInt(id));
            }
            break;
            case "getDeployTypes":
            {
                String projectId = req.getParameter("projectId");
                ret = deployService.getDeployTypes(projectId);
            }
            break;
            case "addDeployForm":
            {
                String projectId = req.getParameter("projectId");
                String id = req.getParameter("id");
                String name = req.getParameter("name");
                String codeOrigin = req.getParameter("codeOrigin");
                String buildTask = req.getParameter("buildTask");

                String[] fixParamKey = req.getParameterMap().get("fixParamKey");
                JSONObject fixParamJson = new JSONObject();
                if(fixParamKey != null && fixParamKey.length>0){
                    String[]  fixParamValue = req.getParameterMap().get("fixParamValue");
                    for(int i=0;i<fixParamKey.length;i++){
                        if(StringUtils.isNotEmpty(fixParamKey[i])){
                            fixParamJson.put(fixParamKey[i],fixParamValue[i]);
                        }
                    }
                }

                String[] packParamKey = req.getParameterMap().get("packParamKey");
                JSONArray packParamKeyJson = new JSONArray();
                if(packParamKey != null && packParamKey.length>0){
                    for(String key :packParamKey){
                        if(StringUtils.isNotEmpty(key)) {
                            packParamKeyJson.add(key);
                        }
                    }
                }


                String restartStr = req.getParameter("restart");
                int restart = "on".equals(restartStr)?1:0;

                ret = deployService.addDeployType(projectId,id,name,Integer.parseInt(codeOrigin),buildTask,fixParamJson.toJSONString(),packParamKeyJson.toJSONString(),restart);
            }
                break;
            case "delDeployForm":
            {
                String projectId = req.getParameter("projectId");
                String id = req.getParameter("id");
                ret = deployService.delDeployType(projectId,id);
            }
            break;
            case "addServerForm":
                {
                    // String projectId, int id, String name, String sshIp, String sshUser, String sshPassword, String path
                    String projectId = req.getParameter("projectId");
                    String id = req.getParameter("id");
                    String name = req.getParameter("name");
                    String sshIp = req.getParameter("sshIp");
                    String sshUser = req.getParameter("sshUser");
                    String sshPassword = req.getParameter("sshPassword");
                    String path = req.getParameter("path");
                    String isReplaceIdStr = req.getParameter("isReplaceId");
                    String isReplaceNameStr = req.getParameter("isReplaceName");

                    System.out.println(req.getParameterMap());
                    // isReplaceId
                    boolean isReplaceId = "on".equals(isReplaceIdStr);
                    boolean isReplaceName = "on".equals(isReplaceNameStr);

                    String[] configKeys = req.getParameterMap().get("configkey");
                    Map<String,String> configMap = new HashMap<>();
                    if(isReplaceId){
                        configMap.put("serverId",id);
                    }
                    if(isReplaceName){
                        configMap.put("serverName",name);
                    }
                    if(configKeys != null && configKeys.length>0){
                        String[] configValues = req.getParameterMap().get("configvalue");
                        for(int i=0;i<configKeys.length;i++){
                            if(StringUtils.isNotEmpty(configKeys[i].trim())){
                                configMap.put(configKeys[i],configValues[i]);
                            }
                        }
                    }

                    ret = deployService.addDeployServer(projectId,Integer.parseInt(id),name,sshIp,sshUser,sshPassword,path,configMap);
                }

                break;
            case "delServerForm":
            {
                // String projectId, int id, String name, String sshIp, String sshUser, String sshPassword, String path
                String projectId = req.getParameter("projectId");
                String id = req.getParameter("id");
                String page = req.getParameter("page");
                ret = deployService.delDeployServer(projectId,Integer.parseInt(id),Integer.parseInt(page));
            }

            break;
            case "getDeployServerList":
            {
                // projectId int id,String name,String innerHost,String publicHost,int netEventPort,int requestPort,int type
                String projectId = req.getParameter("projectId");
                String start = req.getParameter("start");
                String end = req.getParameter("end");
                ret = deployService.getDeployServerList(projectId,0,deployService.serverPageSize);
            }
            break;
            case "doDeploy":
            {
                // {deployRestart=[1],deployCodeOrigin=[1],deployBuildParams=[],oper=[doDeploy],id=[1],deployEnv=[online],projectId=[差不多英雄],serverIds=[5]}
                System.out.println(req.getParameterMap());
                String projectId = req.getParameter("projectId");
                String deployId = req.getParameter("deployId");
                String serverIds = req.getParameter("serverIds");
                String[] packParam = req.getParameterMap().get("packParam");

                ret = deployService.doDeploy(projectId,Integer.parseInt(deployId),serverIds,packParam);
                break;
            }
            case "getDeployState":
            {
                System.out.println(req.getParameterMap());
                String projectId = req.getParameter("projectId");
                String deployId = req.getParameter("deployId");
                String logRow = req.getParameter("logRow");
                ret = deployService.getDeployStateForClient(projectId,Integer.parseInt(deployId),Integer.parseInt(logRow));
            }

            break;
            default:
                throw new MMException("oper is error,oper="+oper);
        }
        System.out.println(ret);
        return ret;
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
