package com.peony.platform.deploy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.*;
import com.peony.engine.framework.cluster.ServerInfo;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.security.exception.ToClientException;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.platform.deploy.util.FileProgressMonitor;
import com.peony.platform.deploy.util.JschUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DeployService {
    private static final Logger logger = LoggerFactory.getLogger(DeployService.class);
    private DataService dataService;

    final String endStr = "command end exit";

    private ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<Integer,String> deployStateMap = new ConcurrentHashMap<>(); // 各个服务器部署的状态提示
//    private ConcurrentHashMap<Integer,String> deployStateMap = new ConcurrentHashMap<>(); // 各个服务器部署的状态提示

    @Gm(id = "deploytest")
    public void gm(){
        //

    }

    public static void main(String[] args) throws Exception{

        List<ServerInfo> serverInfos = new ArrayList<>();
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setId(1);
        serverInfos.add(serverInfo);
//        serverInfo.setInnerHost();
        new DeployService().deployLocal("test","/usr/my",serverInfos);
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
    public void deployGit(String gitUrl){

    }

    public void deployLocal(String env,String targetDir,List<ServerInfo> serverInfos)throws Exception{
        deployStateMap.clear();

        // 本地编译
        String projectUrl = System.getProperty("user.dir");

        StringBuilder cmd = new StringBuilder("cd "+projectUrl+" \n");
        cmd.append("pwd \n");
        cmd.append("echo compile... \n");
        cmd.append("gradle  build_param -P env="+env+" \n");
        //tar -xzvf im_toby.tar.gz
        cmd.append("cd "+projectUrl+"/build \n");
        cmd.append("tar -czvf "+projectUrl+"/build/target.tar.gz "+"./target"); // TODO 最后要删除

        String[] cmds = {"/bin/sh","-c",cmd.toString()};

        Process pro = Runtime.getRuntime().exec(cmds);
//        pro.waitFor();
        InputStream in = pro.getInputStream();
        BufferedReader read = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while((line = read.readLine())!=null){
            System.out.println(line);
            logQueue.offer(line);
        }
        Thread.sleep(500);
        logQueue.offer(endStr);

        // sudo launchctl load -w /System/Library/LaunchDaemons/ssh.plist
        // sudo launchctl list | grep ssh

        ExecutorService executorService =Executors.newFixedThreadPool(serverInfos.size()<32?serverInfos.size():32);

        //
        // 上传
        for(final ServerInfo serverInfo : serverInfos){
            executorService.execute(()->{
                try{
                    deployStateMap.put(serverInfo.getId(),"开始连接");
                    // 连接
//                    Session session  = this.connect("localhost","郑玉振elex",22,"zhengyuzhen");
                    Session session  = connect("47.93.249.150","root",22,"Zyz861180416");
            //            Session session  = this.connect(serverInfo.getSshIp(),serverInfo.getSshUser(),22,serverInfo.getSshPassword());
                    System.out.println("isConnected:"+session!=null);

                    // 创建目录
                    deployStateMap.put(serverInfo.getId(),"创建目录");
                    StringBuilder uploadCmds = new StringBuilder();
                    uploadCmds.append("mkdir -p "+targetDir+" \n");
                    uploadCmds.append("cd "+targetDir+" \n");
                    execCmd(session,uploadCmds.toString());
                    // 上传
                    deployStateMap.put(serverInfo.getId(),"正在上传");
                    upload(session,targetDir+"/target.tar.gz",projectUrl+"/build/target.tar.gz",new DeployProgressSetter(deployStateMap,serverInfo.getId()));

                    // 解压并执行
                    deployStateMap.put(serverInfo.getId(),"解压并执行");
                    StringBuilder execCmds = new StringBuilder();
                    execCmds.append("cd "+targetDir+" \n");
                    execCmds.append("tar -xzvf target.tar.gz \n");
                    execCmds.append("cd target \n");
                    execCmds.append("sh start.sh \n");
                    execCmd(session,execCmds.toString(),true);
                    // 断开
                    session.disconnect();
                }catch (Throwable e){
                    logger.error("deploy server error! server id={} ",serverInfo.getId(),e);
                }
            });
        }
        Thread.sleep(500);
        logger.info("end----111");
    }

    public void deleteServer(int id){
        ServerInfo serverInfo = dataService.selectObject(ServerInfo.class,"id=?",id);
        if(serverInfo == null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"server id is not exist! id={}",id);
        }
        dataService.delete(serverInfo);
    }

    public void addServer(int id,String name,String innerHost,String publicHost,int netEventPort,int requestPort,int type,int verifyServer){
        ServerInfo serverInfo = dataService.selectObject(ServerInfo.class,"id=?",id);
        if(serverInfo != null){
            throw new ToClientException(SysConstantDefine.InvalidParam,"server id has exist! id={}",id);
        }
        serverInfo = new ServerInfo();
        serverInfo.setId(id);
        serverInfo.setName(name);
        serverInfo.setInnerHost(innerHost);
        serverInfo.setPublicHost(publicHost);
        serverInfo.setNetEventPort(netEventPort);
        serverInfo.setRequestPort(requestPort);
        serverInfo.setType(type);
        serverInfo.setVerifyServer(verifyServer);
        dataService.insert(serverInfo);
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

    /**
     * 连接到指定的服务器
     * @return
     * @throws JSchException
     */
    public Session connect(String jschHost,String jschUserName,int jschPort,String jschPassWord) throws JSchException {

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

        }catch(Exception e){
            logger.error(e.getMessage(), e);
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
    public void upload(Session session,String directory, String uploadFile,DeployProgressSetter deployProgressSetter) {
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
            OutputStream out = chSftp.put(directory, new FileProgressMonitor(fileSize,deployProgressSetter), ChannelSftp.OVERWRITE); // 使用OVERWRITE模式
            byte[] buff = new byte[1024 * 256]; // 设定每次传输的数据块大小为256KB
            int read;
            if (out != null) {
                logger.info("Start to read input stream");
                InputStream is = new FileInputStream(uploadFile);
                do {
                    read = is.read(buff, 0, buff.length);
                    if (read > 0) {
                        out.write(buff, 0, read);
                    }
                    out.flush();
                } while (read >= 0);
                logger.info("input stream read done.");
            }

            // chSftp.put(uploadFile, directory, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); //方法二
            // chSftp.put(new FileInputStream(src), dst, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); //方法三
            logger.info("成功上传文件至"+directory);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            chSftp.quit();

            if (channel != null) {
                logger.info("channel disconnect begin");
                channel.disconnect();
                logger.info("channel disconnect end");
            }

        }
    }

    public void execCmd(Session session,String cmd){
        execCmd(session,cmd,false);
    }
    public void execCmd(Session session,String cmd,boolean startUp){
        try{
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.connect();
            InputStream inputStream = channel.getInputStream();
            OutputStream outputStream = channel.getOutputStream();

            outputStream.write(cmd.getBytes());

            String cmd6 = "exit \n\r";
            outputStream.write(cmd6.getBytes());

            outputStream.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

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
                        break;
                    }
                }
                System.out.println(msg);
            }
            in.close();
        }catch (Throwable e){
            e.printStackTrace();
        }


    }

    public static class DeployProgressSetter{
        Map<Integer,String> map;
        int id;
        DeployProgressSetter(Map<Integer,String> map,int id){
            this.map = map;
            this.id = id;
        }
        public void set(String msg){
            map.put(id,msg);
        }
    }
}
