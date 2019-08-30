package com.peony.engine.framework.control.service.rule;

import java.util.List;

public interface ServiceCallRule {
    int failGetNextServerId(int serverId,List<Integer> failIds);
}
