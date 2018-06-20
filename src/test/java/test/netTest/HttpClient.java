package test.netTest;

import com.peony.engine.framework.net.packet.HttpPBPacket;
import com.peony.engine.framework.server.SysConstantDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class HttpClient {
	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	private static HttpClient ioServer=new HttpClient();
	public static HttpClient getInstance(){
		return ioServer;
	}
	// 一个访问认为的超时次数
	private static final int OVERTIME=50000;
	// 一个连接认为的超时时间，如果连接超时，尝试重新连接，一直到连接总时间大于OVERTIME
	private static final int TIMEOUT=4000;
	// 尝试连接的次数
	private static final int TRYCONNECTIONTIMES=10;

	private HttpClient(){
	}
	private String urlStr="http://127.0.0.1:8080/";
	private static final String KEY_GAME_SESSION= SysConstantDefine.sessionId;
	private static final String KEY_GAME_OPCODE = SysConstantDefine.opcodeKey;
	/*
	 * 返回一个PBPacket值,对应result值
	 * 如果成功，返回1
	 * 异常返回0,包括连接超时
	 * 服务器超时，返回-1
	 * 错误，返回-2
	 * 返回opcode
	 *
	 * 这里的实现是有问题的，如果先返回了，就不会被notify了，要用CountDownLatch
	*/
	public HttpPBPacket send(final HttpPBPacket packet, final String session){
		final HttpPBPacket rePacket = new HttpPBPacket(packet.getOpcode(), new byte[0]);
		rePacket.setResult(-1);
//		final Thread thisThread=Thread.currentThread();
		final CountDownLatch latch = new CountDownLatch(1);
		Thread thread=new Thread(){
			HttpURLConnection connection=null;
			int tryConnectTimes=0;
			@Override
			public void run(){
				while(true){
					try{
						URL url=new URL(urlStr);

						connection=(HttpURLConnection)url.openConnection();
						connection.setConnectTimeout(TIMEOUT);
						connection.setDoOutput(true);
						connection .setRequestProperty("Accept-Encoding", "identity");
//						connection.setc
						connection.setRequestProperty("controller", "DefaultRequestController");
						connection.setRequestProperty(KEY_GAME_OPCODE, ""+packet.getOpcode());
						connection.setRequestProperty(KEY_GAME_SESSION, session==null?"":session);
						connection.setRequestMethod("POST");
						BufferedOutputStream out=new BufferedOutputStream(connection.getOutputStream());

						byte[] data=packet.getData();
						out.write(data,0,data.length);
						out.flush();
						out.close();

						InputStream is = connection.getInputStream();

						int bufSize=connection.getContentLength();
						if(bufSize < 0){
							logger.info("server return bufsize:"+bufSize);
							rePacket.setResult(-2);
							latch.countDown();
							return ;
						}
						byte[] buffer = new byte[bufSize];
						int size = is.read(buffer);
						int readedSize = size;
						if (size != bufSize) {
							while (size > -1) {
								size = is.read(buffer, readedSize, bufSize - readedSize);
								readedSize += size;
							}
						}
						String opcodeStr = connection.getHeaderField(KEY_GAME_OPCODE);
//						String sessionStr= connection.getHeaderField(KEY_GAME_SESSION);
						if (opcodeStr == null) {
							rePacket.setResult(-2);
							latch.countDown();
							return ;
						}
						int opcode = Integer.parseInt(opcodeStr);
						if(packet.getOpcode()==opcode){
							rePacket.setResult(1);
						}else{
							rePacket.setResult(opcode);
						}
//						rePacket.setSession(sessionStr);
						rePacket.setOpcode(opcode);
						rePacket.setData(buffer);
						latch.countDown();
						return;
					}catch(IOException e){
						if(e instanceof ConnectException || e instanceof SocketTimeoutException || e instanceof SocketException){
							if(tryConnectTimes<TRYCONNECTIONTIMES){
								tryConnectTimes++;
								logger.info("CONNECTnum:"+tryConnectTimes+"----------------------------------------------------------");
								continue;
							}
						}
						e.printStackTrace();
						rePacket.setResult(0);
						latch.countDown();
						return;
					}finally{
						connection.disconnect();
					}
				}
			}
		};
		thread.start();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return rePacket;
	}
}
