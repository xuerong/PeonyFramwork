package com.peony.platform.deploy.util;

import java.util.Map;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
/**
 * 注释
 *
 * @Author： coding99
 * @Date： 16-9-2
 * @Time： 下午7:33
 */
public class JschUtil {

    private static final Logger logger = LoggerFactory.getLogger(JschUtil.class);

    private String charset = "UTF-8"; // 设置编码格式,可以根据服务器的编码设置相应的编码格式
    private JSch jsch;
    private Session session;
    Channel channel = null;
    ChannelSftp chSftp = null;


    //初始化配置参数
    private String jschHost = "47.93.249.150";
    private int jschPort = 22;
    private String jschUserName = "root";
    private String jschPassWord = "Zyz861180416";
    private int jschTimeOut = 5000;



    /**
     * 静态内部类实现单例模式
     */
    private static class LazyHolder {
        private static final JschUtil INSTANCE = new JschUtil();
    }

    private JschUtil() {

    }

    /**
     * 获取实例
     * @return
     */
    public static final JschUtil getInstance() {
        return LazyHolder.INSTANCE;
    }


    /**
     * 连接到指定的服务器
     * @return
     * @throws JSchException
     */
    public boolean connect() throws JSchException {

        jsch = new JSch();// 创建JSch对象

        boolean result = false;

        try{

            long begin = System.currentTimeMillis();//连接前时间
            logger.debug("Try to connect to jschHost = " + jschHost + ",as jschUserName = " + jschUserName + ",as jschPort =  " + jschPort);

            session = jsch.getSession(jschUserName, jschHost, jschPort);// // 根据用户名，主机ip，端口获取一个Session对象
            session.setPassword(jschPassWord); // 设置密码
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);// 为Session对象设置properties
            session.setTimeout(jschTimeOut);//设置连接超时时间
            session.connect();

            logger.debug("Connected successfully to jschHost = " + jschHost + ",as jschUserName = " + jschUserName + ",as jschPort =  " + jschPort);

            long end = System.currentTimeMillis();//连接后时间

            logger.debug("Connected To SA Successful in {} ms", (end-begin));

            result = session.isConnected();

        }catch(Exception e){
            logger.error(e.getMessage(), e);
        }finally{
            if(result){
                logger.debug("connect success");
            }else{
                logger.debug("connect failure");
            }
        }

        if(!session.isConnected()) {
            logger.error("获取连接失败");
        }

        return  session.isConnected();

    }

    /**
     * 关闭连接
     */
    public void close() {

        if(channel != null && channel.isConnected()){
            channel.disconnect();
            channel=null;
        }

        if(session!=null && session.isConnected()){
            session.disconnect();
            session=null;
        }

    }

    /**
     * 脚本是同步执行的方式
     * 执行脚本命令
     * @param command
     * @return
     */
    public Map<String,Object> execCmmmand(String command) throws Exception{


        Map<String,Object> mapResult = new HashMap<String,Object>();

        logger.debug(command);

        StringBuffer result = new StringBuffer();//脚本返回结果

        BufferedReader reader = null;
        int returnCode = -2;//脚本执行退出状态码


        try {

            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            channel.connect();//执行命令 等待执行结束

            InputStream in = channel.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in, Charset.forName(charset)));

            String res="";
            while((res=reader.readLine()) != null){
                result.append(res+"\n");
                logger.debug(res);
            }

            returnCode = channel.getExitStatus();

            mapResult.put("returnCode",returnCode);
            mapResult.put("result",result.toString());

        } catch (IOException e) {

            logger.error(e.getMessage(),e);

        } catch (JSchException e) {

            logger.error(e.getMessage(), e);

        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return mapResult;

    }

    /**
     * 上传文件
     *
     * @param directory 上传的目录,有两种写法
     *                  １、如/opt，拿到则是默认文件名
     *                  ２、/opt/文件名，则是另起一个名字
     * @param uploadFile 要上传的文件 如/opt/xxx.txt
     */
    public void upload(String directory, String uploadFile) {

        try {

            logger.debug("Opening Channel.");
            channel = session.openChannel("sftp"); // 打开SFTP通道
            channel.connect(); // 建立SFTP通道的连接
            chSftp = (ChannelSftp) channel;
            File file = new File(uploadFile);
            long fileSize = file.length();

            /*方法一*/
            OutputStream out = chSftp.put(directory, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); // 使用OVERWRITE模式
            byte[] buff = new byte[1024 * 256]; // 设定每次传输的数据块大小为256KB
            int read;
            if (out != null) {
                logger.debug("Start to read input stream");
                InputStream is = new FileInputStream(uploadFile);
                do {
                    read = is.read(buff, 0, buff.length);
                    if (read > 0) {
                        out.write(buff, 0, read);
                    }
                    out.flush();
                } while (read >= 0);
                logger.debug("input stream read done.");
            }

            // chSftp.put(uploadFile, directory, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); //方法二
            // chSftp.put(new FileInputStream(src), dst, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); //方法三
            logger.debug("成功上传文件至"+directory);

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            chSftp.quit();

            if (channel != null) {
                channel.disconnect();
                logger.debug("channel disconnect");
            }

        }
    }


    /**
     * 下载文件
     *
     * @param directory 下载的目录,有两种写法
     *                  １、如/opt，拿到则是默认文件名
     *                  ２、/opt/文件名，则是另起一个名字
     * @param downloadFile 要下载的文件 如/opt/xxx.txt
     *
     */

    public void download(String directory, String downloadFile) {
        try {

            logger.debug("Opening Channel.");
            channel = session.openChannel("sftp"); // 打开SFTP通道
            channel.connect(); // 建立SFTP通道的连接
            chSftp = (ChannelSftp) channel;
            SftpATTRS attr = chSftp.stat(downloadFile);
            long fileSize = attr.getSize();


            OutputStream out = new FileOutputStream(directory);

            InputStream is = chSftp.get(downloadFile, new MyProgressMonitor());
            byte[] buff = new byte[1024 * 2];
            int read;
            if (is != null) {
                logger.debug("Start to read input stream");
                do {
                    read = is.read(buff, 0, buff.length);
                    if (read > 0) {
                        out.write(buff, 0, read);
                    }
                    out.flush();
                } while (read >= 0);
                logger.debug("input stream read done.");
            }

            //chSftp.get(downloadFile, directory, new FileProgressMonitor(fileSize)); // 代码段1
            //chSftp.get(downloadFile, out, new FileProgressMonitor(fileSize)); // 代码段2

            logger.debug("成功下载文件至"+directory);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            chSftp.quit();
            if (channel != null) {
                channel.disconnect();
                logger.debug("channel disconnect");
            }
        }
    }


    /**
     * 删除文件
     * @param deleteFile 要删除的文件
     */
    public void delete(String deleteFile) {

        try {
            connect();//建立服务器连接
            logger.debug("Opening Channel.");
            channel = session.openChannel("sftp"); // 打开SFTP通道
            channel.connect(); // 建立SFTP通道的连接
            chSftp = (ChannelSftp) channel;
            chSftp.rm(deleteFile);
            logger.debug("成功删除文件"+deleteFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }


    public void test(){
        try{
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.connect();
            InputStream inputStream = channel.getInputStream();
            OutputStream outputStream = channel.getOutputStream();
            String cmd = "ls \n\r";
            outputStream.write(cmd.getBytes());
            String cmd2 = "cd /usr/myfruit/target \n\r";
            outputStream.write(cmd2.getBytes());
            String cmd3 = "pwd \n\r";
            outputStream.write(cmd3.getBytes());

            String cmd4 = "sh start.sh \n\r";
            outputStream.write(cmd4.getBytes());
            String cmd5 = "pwd \n\r";
            outputStream.write(cmd5.getBytes());

            outputStream.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

            String msg = null;
            while((msg = in.readLine())!=null){
                System.out.println(msg);
            }
            in.close();
        }catch (Throwable e){
            e.printStackTrace();
        }


    }



    public static void main(String[] args) throws Exception{

        JschUtil jschUtil = JschUtil.getInstance();

        boolean isConnected = false;
        isConnected  = jschUtil.connect();
        System.out.println("isConnected:"+isConnected);

        if(isConnected == true){
//            jschUtil.


//            /*上传文件*/
//            jschUtil.upload("/home/README2.md","README.md");
//
            jschUtil.test();
//             /*执行命令*/
//            String command = "ls -ltr /opt";
//            // String command = ShellConfigUtil.getShell("ls");
//            Map<String,Object> result = jschUtil.execCmmmand(command);
//            System.out.println(result.get("result").toString());
//
//            /*下载文件*/
//            jschUtil.download("/opt/123456.png","/opt/123456.png");
//
            jschUtil.close();

        }


    }
}


