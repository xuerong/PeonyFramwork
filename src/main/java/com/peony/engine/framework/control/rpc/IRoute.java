package com.peony.engine.framework.control.rpc;

public interface IRoute {
    Class<?> getFirstArgType();
    int getServerId(Object para);
}
