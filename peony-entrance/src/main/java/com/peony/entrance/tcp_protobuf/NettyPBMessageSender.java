package com.peony.entrance.tcp_protobuf;

import com.peony.core.control.BeanHelper;
import com.peony.core.control.request.RequestService;
import com.peony.core.data.entity.account.MessageSender;
import com.peony.core.net.packet.NettyPBPacket;
import com.peony.common.tool.thread.ThreadPoolHelper;
import io.netty.channel.Channel;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by apple on 16-10-4.
 */
public class NettyPBMessageSender implements MessageSender{
    private static final Logger log = LoggerFactory.getLogger(NettyPBMessageSender.class);
    private static final ScheduledExecutorService asyncExecutor = ThreadPoolHelper.newScheduledThreadPoolExecutor("NettyPBMessageSender",32);
    private Channel channel;
    private String accountId;
    private RequestService requestService;

    public NettyPBMessageSender(Channel channel,String accountId){
        this.channel = channel;
        requestService = BeanHelper.getServiceBean(RequestService.class);
        this.accountId = accountId;
    }

    @Override
    public void sendMessage(final int opcode, final Object data){
        asyncExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMessageSync(opcode,data);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
        });
    }
    @Override
    public void sendMessageSync(int opcode,Object data){
        synchronized (this) {
            NettyPBPacket nettyPBPacket = new NettyPBPacket();
            nettyPBPacket.setId(-1); // 没有的时候为-1
            nettyPBPacket.setData((byte[])data);
            nettyPBPacket.setOpcode(opcode);
            channel.writeAndFlush(nettyPBPacket);
            log.info("send info,cmd = {}|accountId={}",requestService.getOpcodeNames().get(opcode),accountId);
        }
    }
}
