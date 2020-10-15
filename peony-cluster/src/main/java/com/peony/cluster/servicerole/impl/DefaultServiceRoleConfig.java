package com.peony.cluster.servicerole.impl;

import com.peony.cluster.servicerole.IServiceRoleConfig;
import com.peony.cluster.servicerole.RoleNotifier;
import com.peony.cluster.servicerole.ServiceRole;

import java.util.HashMap;
import java.util.Map;

/**
 * @author z84150192
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
