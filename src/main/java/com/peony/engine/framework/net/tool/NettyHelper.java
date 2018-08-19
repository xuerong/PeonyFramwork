package com.peony.engine.framework.net.tool;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by a on 2016/8/29.
 */
public class NettyHelper {
    private static final Logger log = LoggerFactory.getLogger(NettyHelper.class);

    public static synchronized Channel createAndStart(
            final int port,
            final Class<?> encoderClass,
            final Class<?> decoderClass,
            ChannelInboundHandlerAdapter handler,
            String entranceName
    ) throws Exception {
        return createAndStart(port, encoderClass, decoderClass, handler, null, entranceName);
    }

    public static synchronized Channel createAndStart(
            final int port,
            final Class<?> encoderClass,
            final Class<?> decoderClass,
            ChannelInboundHandlerAdapter handler,
            IdleStateHandler idleStateHandler,
            String entranceName
    ) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        NettyThread nettyThread = new NettyThread(port, encoderClass, decoderClass, handler, idleStateHandler, entranceName, latch);
        nettyThread.start();
        latch.await(); // 等待netty启动再放出它
        return nettyThread.getChannel();
    }

    public static synchronized Channel createAndStartWebSocket(
            final int port,
            String entranceName,
            final Class<?>... encoderClass
    ) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        NettyThread nettyThread = new NettyThread(port, entranceName, latch, encoderClass);
        nettyThread.start();
        latch.await(); // 等待netty启动再放出它
        return nettyThread.getChannel();
    }

    private static class NettyThread extends Thread {
        private Channel serverChannel = null;

        final int port;
        Class<?> encoderClass;
        Class<?> decoderClass;
        ChannelInboundHandlerAdapter handler;
        final String entranceName;
        final CountDownLatch latch;
        IdleStateHandler idleStateHandler;

        Class<?>[] webSocketClass;

        public Channel getChannel() {
            return serverChannel;
        }

        public NettyThread(final int port,
                           final Class<?> encoderClass,
                           final Class<?> decoderClass,
                           final ChannelInboundHandlerAdapter handler,
                           String entranceName,
                           CountDownLatch latch) {
            this(port, encoderClass, decoderClass, handler, null, entranceName, latch);
        }

        public NettyThread(final int port,
                           final Class<?> encoderClass,
                           final Class<?> decoderClass,
                           ChannelInboundHandlerAdapter handler,
                           IdleStateHandler idleStateHandler,
                           String entranceName,
                           CountDownLatch latch) {
            this.port = port;
            this.encoderClass = encoderClass;
            this.decoderClass = decoderClass;
            this.handler = handler;
            this.idleStateHandler = idleStateHandler;
            this.entranceName = entranceName;
            this.latch = latch;
        }

        public NettyThread(final int port, String entranceName,
                           CountDownLatch latch, final Class<?>... webSocketClass) {
            this.port = port;
            this.webSocketClass = webSocketClass;
            this.entranceName = entranceName;
            this.latch = latch;
        }

        @Override
        public void run() {
            EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            //final EventExecutorGroup group = new NioEventLoopGroup();
            // 业务线程池
            EventExecutorGroup handlerGroup = new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 4 + 2);
            try {
                ServerBootstrap b = new ServerBootstrap(); // (2)
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class) // (3)
                        .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                if (idleStateHandler != null) {
                                    pipeline.addLast(new IdleStateHandler(
                                            idleStateHandler.getReaderIdleTimeInMillis(),
                                            idleStateHandler.getWriterIdleTimeInMillis(),
                                            idleStateHandler.getAllIdleTimeInMillis(), TimeUnit.MILLISECONDS));
                                }
                                if (webSocketClass != null) {
                                    /**
                                     * HttpServerCodec：将请求和应答消息解码为HTTP消息
                                     HttpObjectAggregator：将HTTP消息的多个部分合成一条完整的HTTP消息
                                     ChunkedWriteHandler：向客户端发送HTML5文件
                                     */
                                    pipeline.addLast("http-codec", new HttpServerCodec());
                                    pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
                                    pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                                    //请求处理
                                    pipeline.addLast(handlerGroup,"inboundHandler", (ChannelInboundHandlerAdapter) webSocketClass[0].newInstance());
                                    //关闭处理
//                                    p.addLast("outboundHandler", webSocketChannelHandlerFactory.newWebSocketOutboundChannelHandler());
                                    //
//                                    pipeline.addLast(new ReadTimeoutHandler(5)); //5秒后未与服务器通信，则断开连接
                                } else {
                                    pipeline.addLast(
                                            (ChannelHandler) decoderClass.newInstance(), // 解码器
                                            (ChannelHandler) encoderClass.newInstance(), // 编码器
                                            handler
                                    );
                                }
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 1024)          // (5)backlog 指定了内核为此套接口排队的最大连接个数
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)


                ChannelFuture f = b.bind(port); // (7)
                f.sync();
                serverChannel = f.channel();
                latch.countDown();
                f.channel().closeFuture().sync();
                log.info(entranceName + " netty stop ");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }
}
