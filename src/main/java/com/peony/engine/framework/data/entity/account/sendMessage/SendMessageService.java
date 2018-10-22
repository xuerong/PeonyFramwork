package com.peony.engine.framework.data.entity.account.sendMessage;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.control.netEvent.remote.RemoteCallService;
import com.peony.engine.framework.control.request.RequestService;
import com.peony.engine.framework.control.rpc.Remotable;
import com.peony.engine.framework.control.rpc.RouteType;
import com.peony.engine.framework.data.entity.account.AccountSysService;
import com.peony.engine.framework.data.entity.account.LogoutEventData;
import com.peony.engine.framework.data.entity.account.MessageSender;
import com.peony.engine.framework.data.entity.session.Session;
import com.peony.engine.framework.data.tx.LockerService;
import com.peony.engine.framework.data.tx.Tx;
import com.peony.engine.framework.server.SysConstantDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private ExecutorService executorService = Executors.newCachedThreadPool();
    /**
     * 用于推送消息的推送器
     * 需要根据使用的协议、传输方式,在创建session的时候设置
     */
    private ConcurrentHashMap<String,MessageSender> messageSenderMap = new ConcurrentHashMap<>();

    private AccountSysService accountSysService;
    private RemoteCallService remoteCallService;
    private LockerService lockerService;
    private RequestService requestService;

    public void init(){

    }
    @EventListener(event = SysConstantDefine.Event_AccountLogin)
    public void login(EventData data){
        Session session = (Session) ((List)data.getData()).get(0);
        MessageSender messageSender = session.getMessageSender();
        if(messageSender != null){
            messageSenderMap.put(session.getAccountId(),messageSender);
        }
    }
    @EventListener(event = SysConstantDefine.Event_AccountLogout)
    public void logout(EventData data){
        LogoutEventData logoutEventData = (LogoutEventData)data.getData();
        if(logoutEventData.getSession().getAccountId() != null) {
            messageSenderMap.remove(logoutEventData.getSession().getAccountId());
        }
    }

    public void sendMessage(String accountId,int opcode,byte[] data){
        doSendMessage(accountId,null,opcode,data);
    }
    public void broadcastMessage(int opcode,byte[] data){
        for(Map.Entry<String,MessageSender> entry : messageSenderMap.entrySet()){
            doSendMessage(entry.getKey(),entry.getValue(),opcode,data);
        }
    }
    private void doSendMessage(String accountId,MessageSender messageSender,int opcode,byte[] data){
        try {
            if(messageSender == null){
                messageSender = messageSenderMap.get(accountId);
            }
            if(messageSender != null){
                messageSender.sendMessage(opcode, data);
            }else{
//                log.info("account not login,accountId="+accountId);
            }
        }catch (Throwable e){
            log.error("broadcast message fail ,opcode = " + opcode+",e = "+e.getMessage()+",accountId+"+accountId);
        }
    }

    // TODO 这个产生大量的远程调用，却很少是需要的，可以有个地方获取在线玩家，只给在线玩家推送
    public void sendMessageRemotable(Collection<String> uids, int opcode, JSONObject data){
        if(uids == null || uids.size() == 0){
            return;
        }
        executorService.execute(()->{
            for(String uid : uids){
                sendMessageRemotable(uid,opcode,data);
            }
        });
    }
    @Remotable(route = RouteType.UID)
    public void sendMessageRemotable(String uid,int opcode,JSONObject data){
        doSendMessage(uid,null,opcode,data);
    }



    public void sendMessage(String accountId,int opcode,JSONObject data){
        doSendMessage(accountId,null,opcode,data);
    }

    private void doSendMessage(String accountId,MessageSender messageSender,int opcode,JSONObject data){
        try {
            if(messageSender == null){
                messageSender = messageSenderMap.get(accountId);
            }
            if(messageSender != null){
                messageSender.sendMessage(opcode, data);
                log.info("user:{} push msg:(msgid:{}[{}]),{}", accountId, requestService.getOpName(opcode),opcode, data.toString() );
            }else{
//                log.info("account not login,accountId="+accountId);
            }
        }catch (Throwable e){
            log.error("broadcast message fail ,opcode = " + opcode+",e = "+e.getMessage()+",accountId+"+accountId);
        }
    }


}
