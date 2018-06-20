package com.peony.requestEntrances.tcp_protobuf;

import com.peony.engine.framework.control.request.RequestService;
import com.peony.engine.framework.data.entity.account.MessageSender;
import com.peony.engine.framework.tool.helper.BeanHelper;
import com.peony.engine.framework.net.packet.NettyPBPacket;
import io.netty.channel.Channel;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by apple on 16-10-4.
 */
public class NettyPBMessageSender implements MessageSender{
    private static final Logger log = LoggerFactory.getLogger(NettyPBMessageSender.class);
    private static final ScheduledExecutorService asyncExecutor = new ScheduledThreadPoolExecutor(100, new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 拒绝执行处理

        }
    }){
        protected void afterExecute(Runnable r, Throwable t) {
            // 执行后处理，注意异常的处理
        }
    };
    private Channel channel;
    private String accountId;
    private RequestService requestService;

    public NettyPBMessageSender(Channel channel,String accountId){
        this.channel = channel;
        requestService = BeanHelper.getServiceBean(RequestService.class);
        this.accountId = accountId;
    }

    @Override
    public void sendMessage(final int opcode, final byte[] data){
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
    public void sendMessageSync(int opcode,byte[] data){
        synchronized (this) {
            NettyPBPacket nettyPBPacket = new NettyPBPacket();
            nettyPBPacket.setId(-1); // 没有的时候为-1
            nettyPBPacket.setData(data);
            nettyPBPacket.setOpcode(opcode);
            channel.writeAndFlush(nettyPBPacket);
            log.info("send info,cmd = {}|accountId={}",requestService.getOpcodeNames().get(opcode),accountId);
        }
    }

    @Override
    public void sendMessage(int opcode, JSONObject data) {
        throw new UnsupportedOperationException("pb send is not support JSONObject");
    }

    @Override
    public void sendMessageSync(int opcode, JSONObject data) {
        throw new UnsupportedOperationException("pb send is not support JSONObject");
    }
}
