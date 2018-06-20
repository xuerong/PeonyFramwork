package com.peony.engine.framework.control.netEvent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputStream;
import java.util.List;

/**
 * Created by a on 2016/8/29.
 * Netty的解码器和http的解码器不同，它要求每个客户端拥有一个实例
 */
public class DefaultNettyDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(DefaultNettyDecoder.class);
    private static final int headSize = 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
        try {
            int readAble = in.readableBytes();
            if (readAble < headSize) {
                return;
            }

            in.markReaderIndex();
            int size = in.readInt();
            if (readAble < (size + headSize)) {
                in.resetReaderIndex();
                return;
            }

            // FIXME wjm 最好改了
            try (ObjectInputStream oin = new ObjectInputStream(new ByteBufInputStream(in.readBytes(size), true))) {
                list.add(oin.readObject());
            }
        } catch (Throwable e) {
            logger.error("decode error ", e);
        }
    }
}
