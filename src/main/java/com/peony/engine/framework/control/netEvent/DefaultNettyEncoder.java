package com.peony.engine.framework.control.netEvent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

/**
 * Created by a on 2016/8/29.
 * Netty的编码器和http的解码器不同，它要求每个客户端拥有一个实例
 */
public class DefaultNettyEncoder extends MessageToByteEncoder {
    private static final Logger logger = LoggerFactory.getLogger(DefaultNettyEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object o, ByteBuf byteBuf) {
        ByteBuf buf = ctx.alloc().buffer();
        try {
            // FIXME  wjm 不安全,容易错!!!!
            try (ObjectOutputStream out = new ObjectOutputStream(new ByteBufOutputStream(buf))) {
                out.writeObject(o);
                out.flush();
            }
            byteBuf.writeInt(buf.readableBytes());
            byteBuf.writeBytes(buf);
        } catch (Throwable e) {
            logger.error("encode error " + o, e);
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }
}
