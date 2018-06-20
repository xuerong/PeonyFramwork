package com.peony.engine.framework.control.event;

/**
 * Created by Administrator on 2015/11/18.
 */
public interface EventListenerHandler {
    public void handle(EventData eventData) throws Exception;
    public void handleSyn(EventData eventData) throws Exception;
}
