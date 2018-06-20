package com.peony.engine.framework.data.entity.account.sendMessage;

import com.peony.engine.framework.data.persistence.orm.annotation.DBEntity;

import java.io.Serializable;

/**
 * Created by apple on 16-10-4.
 */
@DBEntity(tableName = "sendmessagegroup",pks = {"groupId"})
public class SendMessageGroup implements Serializable{
    public static final String Separator = ",";

    private String groupId;
    private String accountIds;// 用,间隔

    public boolean addAccountId(String accountId){
        String[] strs = accountIds.split(Separator);
        for(String str:strs){
            if(str.equals(accountId)){
                return false;
            }
        }
        accountIds+=(Separator+accountId);
        return true;
    }
    public boolean removeAccount(String accountId){
        boolean result = false;
        if(accountIds.contains(accountId)){
            String[] strs = accountIds.split(Separator);
            StringBuilder sb = new StringBuilder();
            for(String str: strs){
                if(!str.equals(accountId)){
                    sb.append(Separator+str);
                }else{
                    result = true;
                }
            }
            if(sb.length()>0){
                accountIds = sb.substring(1);
            }else{
                accountIds = "";
            }
        }
        return result;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(String accountIds) {
        this.accountIds = accountIds;
    }
}
