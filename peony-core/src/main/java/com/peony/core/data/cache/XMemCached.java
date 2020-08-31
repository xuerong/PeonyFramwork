package com.peony.core.data.cache;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.impl.StandardSocketOption;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

public class XMemCached implements IRemoteCacheClient {
	public static final Logger log = LoggerFactory.getLogger(XMemCached.class);
	private String serverlist;

	private long opTimeout = 10000L;

	private int readBufSize = MemcachedClient.DEFAULT_SESSION_READ_BUFF_SIZE;

	private int connectionPoolSize = Runtime.getRuntime().availableProcessors();// ;MemcachedClient.DEFAULT_CONNECTION_POOL_SIZE;
	// ////TCp options
	private boolean tcpNoDelay = true;
	private int tcpSendBuff = MemcachedClient.DEFAULT_TCP_SEND_BUFF_SIZE;
	private int tcpRecvBuff = MemcachedClient.DEFAULT_TCP_RECV_BUFF_SIZE;
	private long sessionIdleTimeout = 300000;// idle 一分钟发心跳
	// // 是否启动监控线程
	// private boolean monitor = false;
	// private int monitorPeriod = 60000;// 1分钟，监控的时间间隔

	private Transcoder<Object> transcoder;
	private int currentIndex = 0;
	private MemcachedClient mcc = null;

	public void setServerlist(String serverlist) {
		this.serverlist = serverlist;
	}

	public void setConnectionPoolSize(int connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}

	public void setSessionIdleTimeout(long sessionIdleTimeout) {
		this.sessionIdleTimeout = sessionIdleTimeout;
	}

	public void setOpTimeout(long opTimeout) {
		this.opTimeout = opTimeout;
	}

	public void setReadBufSize(int readBufSize) {
		this.readBufSize = readBufSize;
	}

	public void setTcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}

	public void setTcpSendBuff(int tcpSendBuff) {
		this.tcpSendBuff = tcpSendBuff;
	}

	public void setTcpRecvBuff(int tcpRecvBuff) {
		this.tcpRecvBuff = tcpRecvBuff;
	}

	public void setTranscoder(Transcoder<Object> transcoder) {
		this.transcoder = transcoder;
	}
	@Override
	public void init() {
		if (mcc != null)
			return;
		log.info("初始化XMemCached,ip:{},readBufSize:{},connectionPoolSize:{}",serverlist,readBufSize,connectionPoolSize);


		try {
//			Configuration.MAX_READ_BUFFER_SIZE = Configuration.MAX_READ_BUFFER_SIZE * 2;

			List<InetSocketAddress> socketAddress = AddrUtil
					.getAddresses(serverlist);
			MemcachedClientBuilder builder = new XMemcachedClientBuilder(
					socketAddress);
			builder.setConnectionPoolSize(connectionPoolSize);
			builder.setSocketOption(StandardSocketOption.TCP_NODELAY,
					tcpNoDelay);
			builder
					.setSocketOption(StandardSocketOption.SO_RCVBUF,
							tcpRecvBuff);
			builder
					.setSocketOption(StandardSocketOption.SO_SNDBUF,
							tcpSendBuff);
			if (this.transcoder == null) {
				this.transcoder = new SerializingTranscoder(10 * 1024 * 1024);// //最大单个数据大小:20M
			}
			builder.setTranscoder(transcoder);
			builder.getConfiguration()
					.setSessionIdleTimeout(sessionIdleTimeout);
			mcc = builder.build();
			mcc.setOpTimeout(this.opTimeout);
			//
			Field shutdownHookThread = null;
			try {
				shutdownHookThread = mcc.getClass().getDeclaredField(
						"shutdownHookThread");
				if (shutdownHookThread != null) {
					shutdownHookThread.setAccessible(true);
					Thread thread = (Thread) shutdownHookThread.get(mcc);
					// shutdownHookThread.set
					if (thread != null) {
						Runtime.getRuntime().removeShutdownHook(thread);
						log.info("删除XMemcached的shutDownHook!");
					}
					thread = new Thread() {
						@Override
						public void run() {
							log.info("修改过的xMemcached shutdown thread,什么也不做....");
							// TDOD 关闭的时候清空memecache,后面要改过来
							
						}
					};
					shutdownHookThread.set(mcc, thread);
					Runtime.getRuntime().addShutdownHook(thread);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			log.info("初始化  memcached client 完毕!");
		} catch (Exception ex) {
			log.debug("初始化  memcached client 出错!");
			log.error(ex.getMessage());
		}

	}

	// public void bindCache2Thread(long id){
	// MemcachedClient c = mcc[(int)(id%mcc.length)];
	// threadLocalMC.set(c);
	// }

	@Override
	public void close() {

		try {
			log.info("mc client shutdown begin....");
			mcc.shutdown();
			log.info("mc client shutdown end....");
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.debug(" memcached client closed");

	}

	@Override
	public boolean set(String key, int exp, Object obj) {
		try {
			return this.mcc.set(key, exp, obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Object get(String key) {
		try {
			return this.mcc.get(key);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public Map<String, Object> getBulk(String[] keys) {
		// long time = System.currentTimeMillis();
		try {
			return this.mcc.get(Arrays.asList(keys));
			// return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public Map<String, Object> getBulk(List<String> keys) {
		try {
			return this.mcc.get(keys);
			// return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean delete(String key) {
		try {
			return this.mcc.delete(key);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long incr(String key, int by) {
		try {
			return this.mcc.incr(key, by);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long decr(String key, int by) {
		try {
			return this.mcc.decr(key, by);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void flush() {
		try {
			mcc.flushAll();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean add(String key, int exp, Object obj) {
		try {
			return this.mcc.add(key, exp, obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteWithNoReply(String key) {
		try {
			// this.mcc.deleteWithNoReply(key);
			this.mcc.delete(key);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void setWithNoReply(String key, int exp, Object obj) {
		try {
			// this.mcc.setWithNoReply(key, exp, obj);
			this.mcc.set(key, exp, obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public static void main(String[] args) throws Throwable{

//		int threadNum = 5;
//		int poolSize = 1;
//		try {
//			threadNum = Integer.parseInt(args[0]);
//			poolSize = Integer.parseInt(args[1]);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		log.info("threadNum=" + threadNum);
//		log.info("poolSize=" + poolSize);
//
//		final XMemCached mc = new XMemCached();
//		mc.setServerlist("172.16.3.53:11211");
//		mc.setConnectionPoolSize(poolSize);
//		mc.init();
//		final CountDownLatch latch = new CountDownLatch(threadNum);
//		long time = System.currentTimeMillis();
//		for (int i = 0; i < threadNum; i++) {
//			new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					String prefix = Thread.currentThread().getName();
//					for (int i = 0; i < 10000; i++) {
//						mc.set(prefix + i, 0, i);
//					}
//
//					for (int i = 0; i < 10000; i++) {
//						Object obj = mc.get(prefix + i);
//					}
//					latch.countDown();
//
//				}
//
//			}).start();
//		}
//		try {
//			latch.await();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		log.info("time=" + (System.currentTimeMillis() - time));
//
//		mc.close();
	}

}
