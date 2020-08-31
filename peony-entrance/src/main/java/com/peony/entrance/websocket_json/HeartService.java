package com.peony.entrance.websocket_json;

import com.alibaba.fastjson.JSONObject;
import com.peony.core.control.annotation.EventListener;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.control.annotation.Updatable;
import com.peony.core.data.entity.account.AccountSysService;
import com.peony.core.data.entity.account.LogoutEventData;
import com.peony.core.data.entity.account.LogoutReason;
import com.peony.core.data.entity.session.Session;
import com.peony.core.server.SysConstantDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service(init = "init")
public class HeartService {
    private static final Logger log = LoggerFactory.getLogger(HeartService.class);

    private AccountSysService accountSysService;
    // accountId - last time
    private Map<String,Long> lastTime = new ConcurrentHashMap<>();
    public void init(){

    }
    @Request(opcode = SysConstantDefine.Heart)
    public JSONObject heart(JSONObject req, Session session){
        lastTime.put(session.getUid(),System.currentTimeMillis());
        return new JSONObject();
    }


    @Updatable(cycle = 300000)
    public void monitorDataBase(int interval){
        Iterator<Map.Entry<String,Long>> it = lastTime.entrySet().iterator();
        long now = System.currentTimeMillis();
        while (it.hasNext()){
            Map.Entry<String,Long> entry = it.next();
            if(now - entry.getValue() > 300000){
                lastTime.remove(entry.getKey());
                // 强制断线
                accountSysService.doLogout(entry.getKey(), LogoutReason.HeartOutTime);
                // 还真有这种情况
                log.warn("注意：hai zhen you zhe zhong qing kuang ,accountId = "+entry.getKey());
            }
        }
    }

    @EventListener(event = SysConstantDefine.Event_AccountLogout)
    public void logout(LogoutEventData logoutEventData) {
        if (logoutEventData.getSession().getUid() != null) {
            lastTime.remove(logoutEventData.getSession().getUid());
        }
    }
}
