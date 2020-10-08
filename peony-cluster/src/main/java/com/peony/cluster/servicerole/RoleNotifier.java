package com.peony.cluster.servicerole;

import java.util.Map;

/**
 * @Author: zhengyuzhen
 * @Date: 2020-10-03 15:31
 */
public interface RoleNotifier {
    void notifier(Map<String,ServiceRole> roleMap);
}
