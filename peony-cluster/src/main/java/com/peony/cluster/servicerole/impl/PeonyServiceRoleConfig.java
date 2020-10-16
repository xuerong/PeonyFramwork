package com.peony.cluster.servicerole.impl;

import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.RoleNotifier;
import com.peony.cluster.servicerole.ServiceRole;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xuerong
 * @since 2020/10/14
 */
public class PeonyServiceRoleConfig implements IServiceRoleConfig {
    /**
     * 从nacos中拿取配置，并
     *
     * @return 服务名-角色id
     */
    @Override
    public Map<String, ServiceRole> getServiceRoles() {
        Map<String, ServiceRole> ret = new HashMap<>();
        if(System.getProperty("serverNum").equals("1")){
            ret.put("com.peony.peony.peonydemo.bag.BagService",ServiceRole.Consumer);
        }else{
            ret.put("com.peony.peony.peonydemo.bag.BagService",ServiceRole.Provider);
        }
        return ret;
    }

    /**
     * 利用nacos的配置通知机制实现服务角色配置的通知
     *
     * @param roleNotifier 通知器
     */
    @Override
    public void subscribe(RoleNotifier roleNotifier) {

    }
}
