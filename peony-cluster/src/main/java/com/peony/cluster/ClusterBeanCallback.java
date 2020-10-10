package com.peony.cluster;

import com.peony.cluster.servicerole.ServiceRole;

import java.util.Map;

public interface ClusterBeanCallback{
    void handle(Map<Class<?>, ServiceRole> serviceRoleMap);
}
