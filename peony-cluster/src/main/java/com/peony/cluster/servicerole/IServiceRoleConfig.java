package com.peony.cluster.servicerole;

import java.util.Map;

public interface IServiceRoleConfig {
    /**
     * 获取Service的角色，
     * 如果没有，默认为ServiceRole.RunSelf
     *
     * @return Service对应的角色，com.xxx.XXXService,1
     */
    Map<String,ServiceRole> getServiceRoles();

    /**
     * 订阅角色变化
     * 当服务角色增删改的时候，通知roleNotifier
     *
     * @param roleNotifier 通知器
     */
    void subscribe(RoleNotifier roleNotifier);
}
