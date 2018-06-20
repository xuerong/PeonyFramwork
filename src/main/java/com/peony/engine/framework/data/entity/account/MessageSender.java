package com.peony.engine.framework.data.entity.account;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by apple on 16-10-4.
 * // TODO 这个要改成两个方法，data改为Object类型
 */
public interface MessageSender {
    public void sendMessage(int opcode,byte[] data);
    public void sendMessageSync(int opcode,byte[] data);
    public void sendMessage(int opcode,JSONObject data);
    public void sendMessageSync(int opcode,JSONObject data);
}
