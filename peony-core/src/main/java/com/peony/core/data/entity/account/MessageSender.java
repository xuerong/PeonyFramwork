package com.peony.core.data.entity.account;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by apple on 16-10-4.
 */
public interface MessageSender {
    public void sendMessage(int opcode,Object data);
    public void sendMessageSync(int opcode,Object data);
}
