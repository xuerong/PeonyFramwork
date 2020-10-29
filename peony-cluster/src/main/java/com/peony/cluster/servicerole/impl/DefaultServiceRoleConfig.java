package com.peony.cluster.servicerole.impl;

import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.RoleNotifier;
import com.peony.cluster.servicerole.ServiceRole;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认的服务角色配置
 * 所有的服务均在自己所在服务机器运行
 *
 * @author xuerong
 * @since 2020/10/15
 */
public class DefaultServiceRoleConfig implements IServiceRoleConfig {
    @Override
    public Map<String, ServiceRole> getServiceRoles() {
        return new HashMap<>();
    }

    @Override
    public void subscribe(RoleNotifier roleNotifier) {

    }
}
