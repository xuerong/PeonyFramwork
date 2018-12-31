package com.peony.platform.deploy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.*;
import com.peony.engine.framework.cluster.ServerInfo;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.security.exception.MMException;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.thread.ThreadPoolHelper;
import com.peony.engine.framework.tool.util.Util;
import com.peony.platform.deploy.util.FileProgressMonitor;
import jdk.nashorn.internal.runtime.regexp.RegExp;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service(destroy = "destroy",init = "init")
public class DeployService {
    private static final Logger logger = LoggerFactory.getLogger(DeployService.class);


    // 一些参数，用于判断服务器启动状态
    public static final String started = "log for deploy service: server start finish";
    public static final String startError = "log for deploy service: server start error";


    private DataService dataService;

    public final int serverPageSize = 10;

    final String endStr = "command end exit";

    public void init(){

    }

    public static void main(String[] args) throws Exception{

//        List<ServerInfo> serverInfos = new ArrayList<>();
//        ServerInfo serverInfo = new ServerInfo();
//        serverInfo.setId(1);
//        serverInfos.add(serverInfo);
////        serverInfo.setInnerHost();
//        new DeployService().deployLocal("test","/usr/my",serverInfos);


        //
//        new DeployService().deployGit("myfruit","https://github.com/xuerong/PeonyFramwork.wiki.git",null,null,"master");

    }

    public JSONObject getCodeOriginList(String projectId){
        List<CodeOrigin> codeOrigins = dataService.selectList(CodeOrigin.class,"projectId=?",projectId);
        codeOrigins.sort(Comparator.comparing(CodeOrigin::getId));
        JSONArray array = new JSONArray();
        for(CodeOrigin codeOrigin : codeOrigins){
            array.add(codeOrigin.toJson());
        }
        JSONObject ret = new JSONObject();
        ret.put("codeOrigins",array);
        return ret;
    }

    public JSONObject addCodeOrigin(String projectId,int id,String name,int type,String params){
        CodeOrigin codeOrigin = dataService.selectObject(CodeOrigin.class,"projectId=? and id=?",projectId,id);
        if(codeOrigin != null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"codeOrigin has exist ,projectId={},id={}",projectId,id);
        }
        codeOrigin = new CodeOrigin();
        codeOrigin.setProjectId(projectId);
        codeOrigin.setName(name);
        codeOrigin.setId(id);
        codeOrigin.setType(type);
        codeOrigin.setParams(params);
        dataService.insert(codeOrigin);
        return getCodeOriginList(projectId);
    }
    public JSONObject delCodeOrigin(String projectId,int id){
        CodeOrigin codeOrigin = dataService.selectObject(CodeOrigin.class,"projectId=? and id=?",projectId,id);
        if(codeOrigin == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"codeOrigin is not exist ,projectId={},id={}",projectId,id);
        }
        dataService.delete(codeOrigin);
        // 删除对应的部署类型
        List<DeployType> deployTypes = dataService.selectList(DeployType.class,"projectId=? and codeOrigin=?",projectId,id);
        for(DeployType deployType:deployTypes){
            dataService.delete(deployType);
        }
        return getCodeOriginList(projectId);
    }


    public JSONObject getDeployProject(){
        List<DeployProject> deployProjects = dataService.selectList(DeployProject.class,"");

        deployProjects.sort(Comparator.comparing(DeployProject::getProjectId));

        JSONArray array = new JSONArray();
        for(DeployProject deployProject : deployProjects){
            array.add(deployProject.toJson());
        }

        JSONObject ret = new JSONObject();
        ret.put("deployProjects",array);
        return ret;
    }

    public JSONObject setDeployProject(String projectId,String name,String defaultSource,String sourceParam){
        DeployProject deployProject = dataService.selectObject(DeployProject.class,"projectId=?",projectId);
        if(deployProject == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"project is not exist ,projectId={}",projectId);
        }
        deployProject.setName(name);
        deployProject.setDefaultSource(defaultSource);
        deployProject.setSourceParam(sourceParam);
        dataService.update(deployProject);
        return getDeployProject();
    }

    public JSONObject addDeployProject(String projectId,String name,String defaultSource,String sourceParam){
        /**
         * private String projectId;
         private String name;
         private String defaultSource;
         @Column(stringColumnType = StringTypeCollation.Text)
         private String sourceParam;
         */
        DeployProject deployProject = dataService.selectObject(DeployProject.class,"projectId=?",projectId);
        if(deployProject != null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"project has exist ,projectId={}",projectId);
        }
        deployProject = new DeployProject();
        deployProject.setProjectId(projectId);
        deployProject.setName(name);
        deployProject.setDefaultSource(defaultSource);
        deployProject.setSourceParam(sourceParam);
        dataService.insert(deployProject);
        return getDeployProject();
    }
    public JSONObject delDeployProject(String projectId){

        DeployProject deployProject = dataService.selectObject(DeployProject.class,"projectId=?",projectId);
        if(deployProject == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"project is not exist ,projectId={}",projectId);
        }
        dataService.delete(deployProject);
        // 删除其它：
        List<CodeOrigin> codeOrigins = dataService.selectList(CodeOrigin.class,"projectId=?",projectId);
        for(CodeOrigin codeOrigin: codeOrigins){
            dataService.delete(codeOrigin);
        }
        List<DeployType> deployTypes = dataService.selectList(DeployType.class,"projectId=?",projectId);
        for(DeployType deployType: deployTypes){
            dataService.delete(deployType);
        }
        List<DeployServer> deployServers = dataService.selectList(DeployServer.class,"projectId=?",projectId);
        for(DeployServer deployServer: deployServers){
            dataService.delete(deployServer);
        }
        return getDeployProject();
    }

    public JSONObject getDeployTypes(String projectId){
        List<DeployType> deployTypes = dataService.selectList(DeployType.class,"projectId=?",projectId);
        deployTypes.sort(Comparator.comparing(DeployType::getId));
        JSONArray jsonArray = new JSONArray();
        for(DeployType deployType:deployTypes){
            jsonArray.add(deployType.toJson());
        }
        JSONObject ret = new JSONObject();
        ret.put("deployTypes",jsonArray);
        return ret;
    }


    public JSONObject addDeployType(String projectId,String id,String name,int codeOrigin,String buildTask,String fixedParam,String packParam,int restart){
        checkParams(projectId,id,name,buildTask);
        DeployProject deployProject = dataService.selectObject(DeployProject.class,"projectId=?",projectId);
        if(deployProject == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"project is not exist ,projectId={}",projectId);
        }
        DeployType deployType = dataService.selectObject(DeployType.class,"projectId=? and id=?",projectId,id);
        if(deployType != null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"id重复");
        }

        deployType = new DeployType();
        deployType.setProjectId(projectId);
        deployType.setId(id);
        deployType.setName(name);
        deployType.setCodeOrigin(codeOrigin);
        deployType.setBuildTask(buildTask);
        deployType.setFixedParam(fixedParam);
        deployType.setPackParam(packParam);
        deployType.setRestart(restart);
        dataService.insert(deployType);
        return getDeployTypes(projectId);
    }
    public JSONObject delDeployType(String projectId,String id){
        checkParams(projectId,id);
        DeployProject deployProject = dataService.selectObject(DeployProject.class,"projectId=?",projectId);
        if(deployProject == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"project is not exist ,projectId={}",projectId);
        }
        DeployType deployType = dataService.selectObject(DeployType.class,"projectId=? and id=?",projectId,id);
        if(deployType == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"deploy type is not exist!projectId=? and id=?",projectId,id);
        }
        dataService.delete(deployType);
        return getDeployTypes(projectId);
    }

    public JSONObject getDeployServerList(String projectId,int pageNum,int pageSize){
        List<DeployServer> deployServers = dataService.selectListBySql(DeployServer.class,"select * from deployserver where projectId=? order by id limit ?,?",projectId,pageSize*pageNum,pageSize);
        deployServers.sort(Comparator.comparing(DeployServer::getId));
        JSONObject ret = new JSONObject();
        JSONArray array = new JSONArray();
        for(DeployServer deployServer: deployServers){
            array.add(deployServer.toJson());
        }
        ret.put("deployServers",array);
        return ret;
    }

    public void deleteServer(int id){
        ServerInfo serverInfo = dataService.selectObject(ServerInfo.class,"id=?",id);
        if(serverInfo == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"server id is not exist! id={}",id);
        }
        dataService.delete(serverInfo);
    }


    public JSONObject addDeployServer(String projectId, int id, String name, String sshIp, String sshUser, String sshPassword, String path,Map<String,String> configMap){

        DeployServer deployServer = dataService.selectObject(DeployServer.class,"projectId=? and id=?",projectId,id);
        if(deployServer != null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"server id has exist!projectId={}, id={}",projectId,id);
        }
        deployServer = new DeployServer();
        deployServer.setProjectId(projectId);
        deployServer.setId(id);
        deployServer.setName(name);
        deployServer.setSshIp(sshIp);
        deployServer.setSshUser(sshUser);
        deployServer.setSshPassword(sshPassword);
        deployServer.setPath(path.trim());
        if(configMap != null && configMap.size()>0){
            JSONObject configJsonObject = new JSONObject();
            for(Map.Entry<String,String> entry:configMap.entrySet() ){
                configJsonObject.put(entry.getKey(),entry.getValue());
            }
            deployServer.setConfig(configJsonObject.toJSONString());
        }

        dataService.insert(deployServer);
        JSONObject ret = getDeployServerList(projectId,0,serverPageSize);
        JSONArray array = ret.getJSONArray("deployServers");
        boolean have = false;
        for(Object object: array){
            JSONObject jsonObject = (JSONObject)object;
            if(jsonObject.getInteger("id")==id){
                have = true;
            }
        }
        if(!have){
            array.add(deployServer.toJson());
        }

        return ret;
    }

    public JSONObject delDeployServer(String projectId, int id,int page){

        DeployServer deployServer = dataService.selectObject(DeployServer.class,"projectId=? and id=?",projectId,id);
        if(deployServer == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"server id is not exist!projectId={}, id={}",projectId,id);
        }
        dataService.delete(deployServer);

        JSONObject ret = getDeployServerList(projectId,page,serverPageSize);
        JSONArray array = ret.getJSONArray("deployServers");
        Iterator<Object> it = array.iterator();
        while (it.hasNext()){
            JSONObject jsonObject = (JSONObject)it.next();
            if(jsonObject.getInteger("id")==id){
                it.remove();
                break;
            }
        }

        return ret;
    }

    public JSONObject doDeploy(String projectId,int deployId,String serverIds,String[] packParam){
        checkParams(projectId,serverIds);
        DeployProject deployProject = dataService.selectObject(DeployProject.class,"projectId=?",projectId);
        if(deployProject == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"project is not exist ,projectId={}",projectId);
        }

        DeployType deployType = dataService.selectObject(DeployType.class,"projectId=? and id=?",projectId,deployId);
        if(deployType == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"deploy type is not exist!projectId={},deployId={}",projectId,deployId);
        }
        //
        CodeOrigin codeOrigin = dataService.selectObject(CodeOrigin.class,"projectId=? and id=?",projectId,deployType.getCodeOrigin());
        if(codeOrigin == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"code origin  is not exist!projectId={},deployId={},codeOrigin={}",projectId,deployId,deployType.getCodeOrigin());
        }


        DeployState deployState = getDeployState(projectId, deployId);
        if (deployState.running.compareAndSet(false, true)) {
            JSONObject params = JSONObject.parseObject(codeOrigin.getParams());

            String projectUrl = null;
            deployState.stateInfo.put("state", 1);
            switch (codeOrigin.getType()){ // 1本地，2git，3svn
                case 1:
                    projectUrl = params.getString("localPath");
                    break;
                case 2: {
                    // 从git拉取
                    projectUrl = fetchFromGit(params.getString("gitPath"), params.getString("gitBranch"), params.getString("gitName"), params.getString("gitPassword"), deployState);
                    break;
                }
                case 3:
                    throw new ToClientException(SysConstantDefine.InvalidParam, "当前不支持");
                default:
                    throw new ToClientException(SysConstantDefine.InvalidParam, "codeOrigin.getType() error!codeOrigin.getType()={}",codeOrigin.getType());
            }
            // 部署参数
            String paramsStr = buildTaskParamsStr(deployType,packParam);
            // 打包和部署
            try {
                deployLocal(projectId, deployType, paramsStr, projectUrl, serverIds, deployState);
            } catch (Exception e) {
                logger.error("", e);
            }
            // 结束
            deployState.reset();
        } else {
            throw new ToClientException(SysConstantDefine.InvalidParam, "on deploy,不能重复部署");
        }
        return new JSONObject();

    }

    private String buildTaskParamsStr(DeployType deployType,String[] packParam){
        // 部署参数
        // packparams
        Map<String,String> packParams = new HashMap<>();
        if(StringUtils.isNotEmpty(deployType.getPackParam())){
            JSONArray array = JSONArray.parseArray(deployType.getPackParam());
            for(int i=0;i<array.size();i++){
                packParams.put((String)array.get(i),packParam[i]);
            }
        }
        // 构造部署参数
        StringBuilder sb = new StringBuilder();

        JSONObject fixedParamJson = JSONObject.parseObject(deployType.getFixedParam());
        for (Map.Entry<String, Object> entry : fixedParamJson.entrySet()) {
            sb.append(" -P " + entry.getKey() + "=" + entry.getValue());
        }

        for (Map.Entry<String, String> entry : packParams.entrySet()) {
            sb.append(" -P " + entry.getKey() + "=" + entry.getValue());
        }
        return sb.toString();
    }


    public JSONObject getServerList(){
        return getServerList(Integer.MIN_VALUE,Integer.MAX_VALUE);
    }
    public JSONObject getServerList(int start,int end){

        List<ServerInfo> serverInfos = dataService.selectListBySql(ServerInfo.class,"select * from serverinfo where id>=? and id<=?",start,end);

        JSONObject ret = new JSONObject();
        JSONArray array = new JSONArray();
        for(ServerInfo serverInfo : serverInfos){
            array.add(serverInfo.toJson());
        }
        ret.put("serverInfos",array);
        return ret;
    }

    private void checkParams(String... params){
        for(String param : params){
            if(StringUtils.isEmpty(param)){
                throw new ToClientException(SysConstantDefine.InvalidParam,"param error,is empty!");
            }
        }
    }



    /**
     * 1、代码来源：从git取（填写git目录），本地直接上传，从svn取，
     * 2、从git或svn取下来，需要列出env下面的目录，作为build的参数
     * 3、执行build
     * 4、上传（压缩吗？sftp还是async？）并启动
     *
     *
     * gradle  build_param -P env=test;
     */
    public String fetchFromGit(String gitUrl, String branch, String credentialsName, String credentialsPassword, DeployState deployState){
        try{
            String projectUrl = System.getProperty("user.dir");

            projectUrl = projectUrl.replace("PeonyFramwork","");

            String localPath = projectUrl+"deploy/git/"+Math.abs(gitUrl.hashCode());
            // 更新本地代码
            gitFetchCode(gitUrl,localPath,branch,credentialsName,credentialsPassword,deployState);
            return localPath;
        }catch (Exception e){
            logger.error("",e);
        }
        return null;
    }



    private void gitFetchCode(String gitUrl,String localPath,String branchName,String credentialsName,String credentialsPassword,DeployState deployState){
        try{
            if (new File(localPath + "/.git").exists()) {
                Git git = Git.open(new File(localPath));
                //检测dev分支是否已经存在 若不存在则新建分支
                List<Ref> localBranch = git.branchList().call();
                boolean isCreate = true;
                for (Ref branch : localBranch) {
                    System.out.println(branch.getName());
                    if (branch.getName().endsWith(branchName)) {
                        isCreate = false;
                        break;
                    }
                }
                git.checkout().setCreateBranch(isCreate).setName(branchName).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setProgressMonitor(new GitMonitor("切换分支"+branchName,deployState)).call();
                PullCommand pullCommand = git.pull();
                if(StringUtils.isNotEmpty(credentialsName)){
                    pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(credentialsName, credentialsPassword));
                }
                pullCommand.setProgressMonitor(new GitMonitor("拉取分支"+branchName,deployState)).call();
            } else {
                List<String> remoteBranch = new ArrayList<>();
                remoteBranch.add("master");
                CloneCommand cloneCommand = Git.cloneRepository();
                if(StringUtils.isNotEmpty(credentialsName)){
                    cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(credentialsName, credentialsPassword));
                }
                Git git =cloneCommand.setURI(gitUrl).setBranchesToClone(remoteBranch).
                        setDirectory(new File(localPath)).setProgressMonitor(new GitMonitor("克隆项目",deployState)).call();
                git.checkout().setCreateBranch(true).setName(branchName).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setProgressMonitor(new GitMonitor("切换分支"+branchName,deployState)).call();
                PullCommand pullCommand = git.pull();
                if(StringUtils.isNotEmpty(credentialsName)){
                    pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(credentialsName, credentialsPassword));
                }
                pullCommand.setProgressMonitor(new GitMonitor("拉取分支"+branchName,deployState)).call();
            }
        }catch (Throwable e){
            logger.error("",e);
        }
    }

    class GitMonitor implements ProgressMonitor {
        private String des;
        private DeployState deployState;
        int completed=0;
        public GitMonitor(String des,DeployState deployState){
            this.des = des;
            this.deployState = deployState;
        }
        @Override
        public void start(int totalTasks) {
            System.out.println("start:"+totalTasks);
            deployState.appendLog(des+" start,totalTasks="+totalTasks);
        }

        @Override
        public void beginTask(String title, int totalWork) {
            System.out.println("beginTask:title="+title+",totalWork="+totalWork);
            deployState.appendLog(des+"beginTask:title="+title+",totalWork="+totalWork);
            completed=0;
        }

        @Override
        public void update(int completed) {
            System.out.println("update:"+completed);
            this.completed += completed;
//            stateInfo.put("update",this.completed);
        }

        @Override
        public void endTask() {
            System.out.println("endTask");
            deployState.appendLog(des+"endTask");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }

    private Map<String,Map<Integer,DeployState>> deployStates = new ConcurrentHashMap<>();

    public JSONObject getDeployStateForClient(String projectId,int deployId,int logRow){
        return getDeployState(projectId, deployId).toClientJson(logRow);
    }
    private DeployState getDeployState(String projectId,int deployId){
        Map<Integer,DeployState> map =  deployStates.get(projectId);
        if(map == null){
            map = new ConcurrentHashMap<>();
            deployStates.putIfAbsent(projectId,map);
            map = deployStates.get(projectId);
        }
        DeployState deployState = map.get(deployId);
        if(deployState == null){
            deployState = new DeployState();
            deployState.projectId = projectId;
            deployState.deployId = deployId;
            map.putIfAbsent(deployId,deployState);
            deployState = map.get(deployId);
        }
        return deployState;
    }

    public void destroy(){

    }


    /**
     * 部署的状态
     * 是否正在部署，部署的信息
     */
    class DeployState{

        public ThreadLocal logPre = new ThreadLocal();

        private String projectId;
        private int deployId;
        private volatile AtomicBoolean running = new AtomicBoolean(false); // 当前的状态，同步代码，打包，部署具体服，
        private JSONObject stateInfo = new JSONObject(); // 这里面存储的状态
        private JSONArray log = new JSONArray(); // 这里面存储的是日志

        private void reset(){
            running.set(false);
            stateInfo = new JSONObject();
            log = new JSONArray();
        }

        public synchronized void appendLog(String log){
            if(logPre.get() != null){
                log = logPre.get()+log;
            }
            this.log.add(log);
        }

        public synchronized  JSONObject toClientJson(int logRow){
            JSONObject ret = stateInfo;
            int size = log.size();
            if(size>logRow){
                ret.put("log",new JSONArray(log.subList(logRow,size)));
                ret.put("logRow",size);
            }else{
                ret.put("log",new JSONArray());
                ret.put("logRow",logRow);
            }
            // 这个地方是防止并发，返回他的copy
            return JSONObject.parseObject(ret.toJSONString());
        }
    }

    public void deployLocal(String projectId,DeployType deployType,String paramsStr,String projectUrl,String serverIds,DeployState deployState)throws Exception{

        deployState.stateInfo.put("state",2);
        // 本地编译
//        String projectUrl = System.getProperty("user.dir");

        StringBuilder cmd = new StringBuilder("cd "+projectUrl+" \n");
        cmd.append("pwd \n");
        cmd.append("echo build... \n");
        cmd.append("gradle  "+deployType.getBuildTask()+paramsStr+" \n");
        //tar -xzvf im_toby.tar.gz
        cmd.append("cd "+projectUrl+"/build \n");
        cmd.append("echo tar begin... \n");
        cmd.append("tar -czvf "+projectUrl+"/build/target.tar.gz "+"./target \n"); // TODO 最后要删除
        cmd.append("echo tar finish... \n");

        System.out.println(cmd);

        String[] cmds = {"/bin/sh","-c",cmd.toString()};

        Process pro = Runtime.getRuntime().exec(cmds);
//        pro.waitFor();
        InputStream in = pro.getInputStream();
        BufferedReader read = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while((line = read.readLine())!=null){
            System.out.println(line);
            deployState.appendLog(line);
        }
        System.out.println("-------------------------------");
        Thread.sleep(500);
        deployState.appendLog(endStr);
        // sudo launchctl load -w /System/Library/LaunchDaemons/ssh.plist
        // sudo launchctl list | grep ssh


        deployState.stateInfo.put("state",3);

        List<String> list = Util.split2List(serverIds,String.class,",");
        StringBuilder sb = new StringBuilder();
        String sp = "";
        for(String _serverId:list){
            if(_serverId.contains("-")){
                List<Integer> fromTo =  Util.split2List(_serverId,Integer.class,"-");
                for(int i=fromTo.get(0);i<=fromTo.get(1);i++){
                    sb.append(sp+i);
                    sp=",";
                }
            }else{
                sb.append(sp+Integer.parseInt(_serverId));
                sp=",";
            }
        }
        String sql = "select * from deployserver where projectId=? and id in ("+sb.toString()+")";
        List<DeployServer> deployServers = dataService.selectListBySql(DeployServer.class,sql,projectId);

        ExecutorService executorService = ThreadPoolHelper.newThreadPoolExecutor("Deploy",32,32,1024);
        //
        JSONArray array = new JSONArray();
        CountDownLatch latch = new CountDownLatch(deployServers.size());
        // 上传
        for(final DeployServer deployServer : deployServers){
            final  JSONObject object = new JSONObject();
            object.put("serverId",deployServer.getId());
            array.add(object);
            executorService.execute(()->{
                deployState.logPre.set(deployServer.getId()+"服\t\t");
                Session session = null;
                try{
                    object.put("st","1"); // 开始连接
                    // 连接
//                    Session session  = this.connect("localhost","郑玉振elex",22,"zhengyuzhen");
//                    Session session  = connect("47.93.249.150","root",22,"Zyz861180416");
                    session  = connect(deployServer.getSshIp(),deployServer.getSshUser(),22,deployServer.getSshPassword());
            //            Session session  = this.connect(deployServer.getSshIp(),deployServer.getSshUser(),22,deployServer.getSshPassword());
                    System.out.println("isConnected:"+session!=null);

                    // 创建目录
                    object.put("st","2");// 正在上传

                    StringBuilder uploadCmds = new StringBuilder();
                    uploadCmds.append("mkdir -p "+deployServer.getPath().trim()+" \n");
                    uploadCmds.append("cd "+deployServer.getPath().trim()+" \n");
                    execCmd(session,uploadCmds.toString(),object,deployState);
                    // 上传
                    upload(session,deployServer.getPath().trim()+"/target.tar.gz",projectUrl+"/build/target.tar.gz",new DeployProgressSetter(deployState,deployServer.getId()));

                    // 解压并执行
                    object.put("st","3"); // 解压并执行
                    StringBuilder execCmds = new StringBuilder();
                    execCmds.append("cd "+deployServer.getPath().trim()+" \n");
                    execCmds.append("tar -xzvf target.tar.gz --strip-components 2 ./target\n");
//                    execCmds.append("cd target \n");
                    // sed -r 's/^\s*serverId\s*=.*/serverId=3/g'
//                    RegExp regExp = new RegExp("");
                    // 修改参数
                    if(StringUtils.isNotEmpty(deployServer.getConfig())){
                        JSONObject configObject = JSONObject.parseObject(deployServer.getConfig());
                        for(Map.Entry<String,Object> entry : configObject.entrySet()){
                            // 这个sed命令在mac上有问题，需要在-i 后面加个空字符串参数：''
                            String replaceCmd = "sed -i 's#^\\s*"+ Util.regReplace(entry.getKey())+"\\s*=.*#"+Util.regReplace(entry.getKey()+"="+entry.getValue())+"#g' config/mmserver.properties \n";
                            execCmds.append(replaceCmd);
                        }
                    }
                    boolean restart = deployType.getRestart()>0;
                    if(restart){
                        execCmds.append("echo begin start server \n");
                        execCmds.append("sh start.sh \n");
                    }else{
                        execCmds.append("echo no need restart server \n");
                    }
                    execCmd(session,execCmds.toString(),restart,object,deployState);
                    // 断开
//                    session.disconnect();
                    //
                }catch (Throwable e){
                    logger.error("deploy server error! server id={} ",deployServer.getId(),e);
                    object.put("error",e.getMessage());
                }finally {
                    latch.countDown();
                    if(session!= null){
                        session.disconnect();
                    }
                    deployState.logPre.remove();
                }
            });
        }
        deployState.stateInfo.put("servers",array);
        latch.await(30,TimeUnit.SECONDS);
        if(latch.getCount()>0){
            logger.error("timeout for deploy");
            deployState.stateInfo.put("error","超时返回，"+latch.getCount()+"个服务器还在部署");
        }
        Thread.sleep(1000); // 等待1秒钟，确保最新的消息返回给了client
        logger.info("end----111");
    }



    /**
     * 连接到指定的服务器
     * @return
     * @throws JSchException
     */
    public Session connect(String jschHost,String jschUserName,int jschPort,String jschPassWord) throws Throwable {

        JSch jsch = new JSch();// 创建JSch对象

        boolean result = false;
        Session session = null;
        try{

            long begin = System.currentTimeMillis();//连接前时间
            logger.info("Try to connect to jschHost = " + jschHost + ",as jschUserName = " + jschUserName + ",as jschPort =  " + jschPort);

            session = jsch.getSession(jschUserName, jschHost, jschPort);// // 根据用户名，主机ip，端口获取一个Session对象
            session.setPassword(jschPassWord); // 设置密码
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);// 为Session对象设置properties
            session.setTimeout(5000);//设置连接超时时间
            session.connect();

            logger.info("Connected successfully to jschHost = " + jschHost + ",as jschUserName = " + jschUserName + ",as jschPort =  " + jschPort);

            long end = System.currentTimeMillis();//连接后时间

            logger.info("Connected To SA Successful in {} ms", (end-begin));

            result = session.isConnected();

        }catch(Throwable e){
            logger.error(e.getMessage(), e);
            throw e;
        }finally{
            if(result){
                logger.info("connect success");
            }else{
                logger.info("connect failure");
            }
        }

        if(!session.isConnected()) {
            logger.error("获取连接失败");
            return null;

        }

        return  session;

    }

    /**
     * 上传文件
     *
     * @param directory 上传的目录,有两种写法
     *                  １、如/opt，拿到则是默认文件名
     *                  ２、/opt/文件名，则是另起一个名字
     * @param uploadFile 要上传的文件 如/opt/xxx.txt
     */
    public void upload(Session session,String directory, String uploadFile,DeployProgressSetter deployProgressSetter) throws Throwable{
        ChannelSftp chSftp = null;
        Channel channel = null;
        try {
            logger.info("Opening Channel.");
            channel = session.openChannel("sftp"); // 打开SFTP通道
            channel.connect(); // 建立SFTP通道的连接
            chSftp = (ChannelSftp) channel;

            File file = new File(uploadFile);
            long fileSize = file.length();

            /*方法一*/
            System.out.println(directory);
            try(OutputStream out = chSftp.put(directory, new FileProgressMonitor(fileSize,deployProgressSetter), ChannelSftp.OVERWRITE)) { // 使用OVERWRITE模式{
                byte[] buff = new byte[1024 * 256]; // 设定每次传输的数据块大小为256KB
                int read;

                logger.info("Start to read input stream");
                InputStream is = new FileInputStream(uploadFile);
                do {
                    read = is.read(buff, 0, buff.length);
                    if (read > 0) {
                        out.write(buff, 0, read);
                    }
                    out.flush();
                } while (read >= 0);

                logger.info("成功上传文件至"+directory);
            }
            // chSftp.put(uploadFile, directory, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); //方法二
            // chSftp.put(new FileInputStream(src), dst, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); //方法三


        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }finally {
            chSftp.quit();

            if (channel != null) {
                logger.info("channel disconnect begin");
                channel.disconnect();
                logger.info("channel disconnect end");
            }

        }
    }

    public void execCmd(Session session,String cmd,JSONObject object,DeployState deployState) throws Throwable{
        execCmd(session,cmd,false,object,deployState);
    }
    public void execCmd(Session session,String cmd,boolean startUp,JSONObject object,DeployState deployState) throws Throwable{
        ChannelShell channel = null;
        try{
            channel = (ChannelShell) session.openChannel("shell");
            channel.connect();
            InputStream inputStream = channel.getInputStream();
            try(OutputStream outputStream = channel.getOutputStream()){
                outputStream.write(cmd.getBytes());

                String cmd6 = "exit \n\r";
                outputStream.write(cmd6.getBytes());

                outputStream.flush();
            }

            try(BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))){
                String msg = null;

                boolean congratulations = false;
                boolean startupSuccess = false;

                while((msg = in.readLine())!=null){
                    if(startUp){
                        if(!congratulations && msg.contains(Server.Congratulations)){
                            congratulations = true;
                        }
                        if(!startupSuccess && msg.contains(Server.startupSuccess)){
                            startupSuccess = true;
                        }
                        if(congratulations && startupSuccess){
                            logger.info("start finish!!!!-------======");
                            object.put("st","5");
                            deployState.appendLog("start finish!!!!-------======");
                            break;
                        }
                        if(msg.endsWith(DeployService.startError)){
                            throw new MMException(MMException.ExceptionType.StartUpFail,"start error!");
                        }
                    }
                    if(msg.contains("begin start server")){
                        object.put("st","4");
                    }
                    if(msg.contains( "no need restart server")){
                        object.put("st","5");
                    }
                    System.out.println(msg);
                    deployState.appendLog(msg);
                }
            }
        }catch (Throwable e){
            e.printStackTrace();
            throw e;
        }finally {
            if(channel!= null){
                channel.disconnect();
            }
        }
    }

    public static class DeployProgressSetter{
        int id;
        DeployState deployState;
        DeployProgressSetter(DeployState deployState,int id){
            this.id = id;
            this.deployState = deployState;
        }
        public void set(String msg){
            this.deployState.stateInfo.put("des",msg);
            this.deployState.appendLog(id+"服\t\t"+msg);
        }
    }
}
