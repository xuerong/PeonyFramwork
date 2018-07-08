package com.peony.engine.framework.control.service;

import java.util.List;

public interface ServiceCallRule {
    public int getServerId(int serverId);
    default int failGetNextServerId(int serverId,List<Integer> failIds){
        return -1;
    }

}
