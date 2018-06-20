package com.peony.requestEntrances.websocket_json;

import com.peony.engine.framework.control.request.RequestService;
import com.peony.engine.framework.data.entity.account.MessageSender;
import com.peony.engine.framework.tool.helper.BeanHelper;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
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
public class WebsocketMessageSender implements MessageSender{
    private static final Logger log = LoggerFactory.getLogger(WebsocketMessageSender.class);
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

    public WebsocketMessageSender(Channel channel, String accountId){
        this.channel = channel;
        requestService = BeanHelper.getServiceBean(RequestService.class);
        this.accountId = accountId;
    }

    @Override
    public void sendMessage(final int opcode, final JSONObject data){
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
    public void sendMessageSync(int opcode, JSONObject data) {
        synchronized (this) {
            JSONObject ret = new JSONObject();
            ret.put("id",opcode);
            ret.put("data",data);
            channel.writeAndFlush(new TextWebSocketFrame(ret.toString()));
            //log.info("user:{} push msg:{}", accountId, data.toString());
            //log.info("send info,cmd = {}|accountId={}",requestService.getOpcodeNames().get(opcode),accountId);
        }
    }

    @Override
    public void sendMessage(int opcode, byte[] data) {
        throw new UnsupportedOperationException("websocket send is not support byte[]");
    }

    @Override
    public void sendMessageSync(int opcode,byte[] data){
        throw new UnsupportedOperationException("websocket send is not support byte[]");
    }
}
