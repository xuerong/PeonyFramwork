package com.peony.engine.framework.data.entity.account.sendMessage;

import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.request.RequestService;
import com.peony.engine.framework.data.entity.account.LogoutEventData;
import com.peony.engine.framework.data.entity.account.MessageSender;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.AbListDataTxLifeDepend;
import com.peony.engine.framework.data.tx.TxCacheService;
import com.peony.engine.framework.server.SysConstantDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by apple on 16-10-4.
 * 推送消息有个主要解决的问题:不同账号登陆在不同的服务器上,必须由对应的服务器推送给它
 * 推送单个玩家原则:
 * 1、先推送自己服务器上的,
 * 2、如果可能存在其它服务器上的,则将其发送给mainServer
 * 3、mainServer分发给对应的服务器进行推送
 *
 * 广播和推送一个组
 *
 *
 */
@Service(init = "init",initPriority = 4)
public class SendMessageService {
    private static final Logger log = LoggerFactory.getLogger(SendMessageService.class);

    /**
     * 用于推送消息的推送器
     * 需要根据使用的协议、传输方式,在创建session的时候设置
     */
    private ConcurrentHashMap<String,MessageSender> messageSenderMap = new ConcurrentHashMap<>();
    private PushTxLifeDepend pushTxLifeDepend = new PushTxLifeDepend();

    private RequestService requestService;
    private TxCacheService txCacheService;

    public void init(){
        txCacheService.registerTxLifeDepend(pushTxLifeDepend);
    }
    @EventListener(event = SysConstantDefine.Event_AccountLogin)
    public void login(List loginEventData){
        Session session = (Session) (loginEventData).get(0);
        MessageSender messageSender = session.getMessageSender();
        if(messageSender != null){
            messageSenderMap.put(session.getUid(),messageSender);
        }
    }
    @EventListener(event = SysConstantDefine.Event_AccountLogout)
    public void logout(LogoutEventData logoutEventData){
        if(logoutEventData.getSession().getUid() != null) {
            messageSenderMap.remove(logoutEventData.getSession().getUid());
        }
    }

    public void broadcastMessage(int opcode,Object data){
        for(Map.Entry<String,MessageSender> entry : messageSenderMap.entrySet()){
            doSendMessage(entry.getKey(),entry.getValue(),opcode,data);
        }
    }

    /**
     * 推送消息给在线的客户端
     * @param uid 玩家id
     * @param opcode 命令号
     * @param data 消息体
     */
    public void sendMessage(String uid,int opcode,Object data){
        MessageSender messageSender = messageSenderMap.get(uid);
        if(messageSender == null){
            return ;
        }
        if(pushTxLifeDepend.checkAndPut(uid, opcode, data)){
            return ;
        }
        doSendMessage(uid,messageSender,opcode,data);
    }

    private void doSendMessage(String uid,MessageSender messageSender,int opcode,Object data){
        try {
            if(messageSender == null){
                messageSender = messageSenderMap.get(uid);
            }
            if(messageSender != null){
                messageSender.sendMessage(opcode, data);
                log.info("user:{} push msg:(msgid:{}[{}]),{}", uid, requestService.getOpName(opcode),opcode, data.toString() );
            }else{
//                log.info("account not login,accountId="+uid);
            }
        }catch (Throwable e){
            log.error("broadcast message fail ,opcode = " + opcode+",e = "+e.getMessage()+",accountId+"+uid);
        }
    }


    class PushTxLifeDepend extends AbListDataTxLifeDepend{
        class PushData{
            String uid;
            int opcode;
            Object data;
        }
        boolean checkAndPut(String uid,int opcode,Object data){
            PushData pushData = new PushData();
            pushData.uid = uid;
            pushData.opcode = opcode;
            pushData.data = data;
            return checkAndPut(pushData);
        }

        @Override
        protected void executeTxCommit(Object object) {
            PushData pushData = (PushData)object;
            sendMessage(pushData.uid,pushData.opcode,pushData.data);
        }
    }

}
