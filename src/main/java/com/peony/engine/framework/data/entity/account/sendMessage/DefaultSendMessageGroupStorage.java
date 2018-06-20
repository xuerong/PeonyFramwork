package com.peony.engine.framework.data.entity.account.sendMessage;

import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.security.exception.MMException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by apple on 16-10-4.
 */
public class DefaultSendMessageGroupStorage implements SendMessageGroupStorage {
    private DataService dataService;

    @Override
    public Map<String, Set<String>> getAllSendMessageGroup() {
        List<SendMessageGroup> sendMessageGroupList = dataService.selectList(SendMessageGroup.class,"");

        //
        Map<String, Set<String>> result = new HashMap<>();
        if(sendMessageGroupList!=null) {
            for (SendMessageGroup sendMessageGroup : sendMessageGroupList) {
                String accountIdsStr = sendMessageGroup.getAccountIds();
                Set<String> accountSet = null;
                if (!StringUtils.isEmpty(accountIdsStr)) {
                    String[] accountIds = accountIdsStr.split(SendMessageGroup.Separator);
                    for (String accountId : accountIds) {
                        accountId = accountId.trim();
                        if (!StringUtils.isEmpty(accountId)) {
                            if (accountSet == null) {
                                accountSet = new HashSet<>();
                            }
                            accountSet.add(accountId);
                        }
                    }
                }
                result.put(sendMessageGroup.getGroupId(), accountSet);
            }
        }
        return result;
    }

    @Override
    public void addAccount(String groupId, String accountId) {
        SendMessageGroup sendMessageGroup = dataService.selectObject(SendMessageGroup.class,"groupId=?",groupId);
        if(sendMessageGroup == null){
            throw new MMException("sendMessageGroup is not exist,groupId="+groupId);
        }
        if(sendMessageGroup.addAccountId(accountId)){
            dataService.update(sendMessageGroup);
        }
    }

    @Override
    public void removeAccount(String groupId, String accountId) {
        SendMessageGroup sendMessageGroup = dataService.selectObject(SendMessageGroup.class,"groupId=?",groupId);
        if(sendMessageGroup != null){
            if(sendMessageGroup.removeAccount(accountId)){
                dataService.update(sendMessageGroup);
            }
        }
    }

    @Override
    public void addGroup(String groupId) {
        SendMessageGroup  sendMessageGroup = new SendMessageGroup();
        sendMessageGroup.setGroupId(groupId);
        sendMessageGroup.setAccountIds("");
        dataService.insert(sendMessageGroup);
    }

    @Override
    public void removeGroup(String groupId) {
        SendMessageGroup sendMessageGroup = dataService.selectObject(SendMessageGroup.class,"groupId=?",groupId);
        if(sendMessageGroup != null) {
            dataService.delete(sendMessageGroup);
        }
    }
}
