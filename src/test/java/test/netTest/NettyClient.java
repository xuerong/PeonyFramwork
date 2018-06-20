package test.netTest;

import com.peony.engine.framework.net.packet.NettyPBPacket;
import com.peony.requestEntrances.tcp_protobuf.RequestNettyPBDecoder;
import com.peony.requestEntrances.tcp_protobuf.RequestNettyPBEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by a on 2016/9/21.
 */
public class NettyClient {
    private static final Logger log = LoggerFactory.getLogger(NettyClient.class);

    private LinkedBlockingQueue<Integer> idOut = new LinkedBlockingQueue<Integer>(); // 可以用的id，id池
    private AtomicInteger idCreator = new AtomicInteger(0); // 如果池中没有可用的，从这里获取

    private Channel channel;
    private String host;
    private int port;

    public NettyClient(String host,int port){
        this.host = host;
        this.port = port;
    }
    public void start() throws Throwable{
        final CountDownLatch latch = new CountDownLatch(1);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                final EventExecutorGroup group = new NioEventLoopGroup();
                try {
                    Bootstrap b = new Bootstrap(); // (1)
                    b.group(workerGroup); // (2)
                    b.channel(NioSocketChannel.class); // (3)
                    b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
                    b.handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new RequestNettyPBEncoder(),
                                    new RequestNettyPBDecoder()
                            );
                            ch.pipeline().addLast(group,"",new NettyClientHandler()); //处理器
                        }
                    });
                    ChannelFuture f = null;
                    // Start the client.
                    while (true) {
                        try {
                            f = b.connect(host, port); // (5) // 这行代码要在while循环里面
                            f.sync();
                            break;
                        } catch (Exception e) {
                            if (e instanceof ConnectException) {
                                log.warn("connect " + "(" + (host + ":" + port) + ") fail ," +
                                        "reconnect after " + 5 + " s");
                                Thread.sleep(5000);
                                continue;
                            }
                        }
                    }
                    channel = f.channel();
                    latch.countDown();
                    f.channel().closeFuture().sync();
                    // 客户端断线
                    log.info("disconnect server:" + "(" + (host + ":" + port) + ")");
//                    int times = 0;
//                    while(backDataMap.size()>0 && times++<10){
//                        Thread.sleep(500);
//                    }
                    if(backDataMap.size()>0){
                        Iterator<BackData> iterator = backDataMap.values().iterator();
                        while (iterator.hasNext()){
                            BackData backData = iterator.next();
                            if(backData.getLatch().getCount()>0) {
                                backData.getLatch().countDown();
                            }
                            iterator.remove();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    workerGroup.shutdownGracefully();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.setName("NettyServerClient");
        thread.start();
        latch.await();
        log.info("connect :"+"("+(host+":"+port) +") success");
    }
    private ConcurrentHashMap<Integer,BackData> backDataMap = new ConcurrentHashMap<>();
    public boolean isConnected(){
        return channel!= null && channel.isActive();
    }
    public byte[] send(int opcode,byte[] data) throws Throwable{
        BackData backData = new BackData();
        backData.setLatch(new CountDownLatch(1));
        Integer id = idOut.poll();
        if(id == null){
            id = idCreator.incrementAndGet();
        }
        backDataMap.put(id,backData);


        NettyPBPacket nettyPBPacket = new NettyPBPacket();
        nettyPBPacket.setId(id);
        nettyPBPacket.setOpcode(opcode);
        nettyPBPacket.setData(data);

        channel.writeAndFlush(nettyPBPacket);
        backData.getLatch().await();
        return backData.getData();
    }
    class NettyClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            NettyPBPacket pbPacket = (NettyPBPacket)msg;
            BackData backData = backDataMap.remove(pbPacket.getId());
            if(backData != null){ // 发送返回的
                backData.setData(pbPacket.getData());
                backData.getLatch().countDown();
                idOut.offer(pbPacket.getId());
            }else{ // 主动推送的

            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
    class BackData{
        private CountDownLatch latch;
        private byte[] data;

        public CountDownLatch getLatch() {
            return latch;
        }

        public void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }
}
