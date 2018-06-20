package com.peony.engine.framework.data.entity.account.sendMessage;

import java.util.Map;
import java.util.Set;

/**
 * Created by apple on 16-10-4.
 */
public interface SendMessageGroupStorage {
    public Map<String,Set<String>> getAllSendMessageGroup();
    public void addAccount(String groupId,String accountId);
    public void removeAccount(String groupId,String accountId);
    public void addGroup(String groupId);
    public void removeGroup(String groupId);
}
