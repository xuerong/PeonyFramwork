package com.peony.engine.framework.control.rpc;


import com.peony.engine.framework.control.ServiceHelper;
import com.peony.engine.framework.control.annotation.EventListener;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.control.event.EventData;
import com.peony.engine.framework.control.netEvent.NettyServerClient;
import com.peony.engine.framework.control.netEvent.remote.RemoteCallService;
import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.server.SysConstantDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.UUID;

@Service
public class RemoteExceptionQueueService {
    private static final Logger log = LoggerFactory.getLogger(RemoteExceptionQueueService.class);

    private DataService dataService;
    private RemoteCallService remoteCallService;

    public void addQueue(int serverId, Class serviceClass, String methodName,String methodSignature, Object[] params, RuntimeException e){
        RemoteExceptionQueue remoteExceptionQueue = new RemoteExceptionQueue();
        remoteExceptionQueue.setId(UUID.randomUUID().toString());
        remoteExceptionQueue.setServerId(serverId);
        remoteExceptionQueue.setServiceClass(serviceClass.getName());
        remoteExceptionQueue.setMethodName(methodName);
        remoteExceptionQueue.setParams(toByteArray(params));
        remoteExceptionQueue.setMethodSignature(methodSignature);
        dataService.insert(remoteExceptionQueue);
        log.info("new remote RemoteExceptionQueue,remoteExceptionQueue={}",remoteExceptionQueue.toString());
    }

    // 用于自动重连后更新 client
    @EventListener(event = SysConstantDefine.Event_ConnectNewServerAsy)
    public void onClientConnected(EventData eventData) {
        NettyServerClient client = (NettyServerClient) eventData.getData();
        List<RemoteExceptionQueue> remoteExceptionQueueList = dataService.selectList(RemoteExceptionQueue.class,"serverId=?",client.getServerId());
        if(remoteExceptionQueueList.size() > 0){
            for(RemoteExceptionQueue remoteExceptionQueue : remoteExceptionQueueList){
                dataService.delete(remoteExceptionQueue);
                remoteCallService.remoteCallAsync(remoteExceptionQueue.getServerId(), ServiceHelper.getServiceClassByName(remoteExceptionQueue.getServiceClass()),
                        remoteExceptionQueue.getMethodName(),remoteExceptionQueue.getMethodSignature(),toObject(remoteExceptionQueue.getParams()));
                log.info("re remoteCall,remoteExceptionQueue={}",remoteExceptionQueue.toString());
            }
        }
    }

    public byte[] toByteArray (Object[] obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray ();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            log.error("toByteArray error,",ex);
        }
        return bytes;
    }

    public Object[] toObject (byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream (bytes);
            ObjectInputStream ois = new ObjectInputStream (bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (IOException ex) {
            log.error("toObject error,",ex);
        } catch (ClassNotFoundException ex) {
            log.error("toObject error,",ex);
        }
        return (Object[])obj;
    }
}
