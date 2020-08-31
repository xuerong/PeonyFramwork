package com.peony.entrance.tcp_protobuf;

import com.peony.core.net.packet.NettyPBPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by a on 2016/9/19.
 */
public class RequestNettyPBEncoder extends MessageToByteEncoder {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object object, ByteBuf byteBuf) throws Exception {
        NettyPBPacket nettyPBPacket = (NettyPBPacket)object;
        byteBuf.writeInt(nettyPBPacket.getData().length);
        byteBuf.writeInt(nettyPBPacket.getOpcode());
        byteBuf.writeInt(nettyPBPacket.getId());
        byteBuf.writeBytes(nettyPBPacket.getData());
    }
}
