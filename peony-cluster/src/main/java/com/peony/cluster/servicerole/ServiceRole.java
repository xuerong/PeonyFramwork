package com.peony.cluster.servicerole;

import com.peony.common.tool.idenum.EnumUtils;
import com.peony.common.tool.idenum.IDEnum;

/**
 * @Author: zhengyuzhen
 * @Date: 2020-10-03 15:20
 */
public enum ServiceRole implements IDEnum {
    None(0),
    Provider(1),
    Consumer(2),
    RunSelf(3),
    ;
    final int id;

    ServiceRole(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ServiceRole valueOf(int id) {
        return EnumUtils.getEnum(ServiceRole.class, id);
    }
}
