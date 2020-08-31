package com.peony.core.configure;

/**
 * Created by a on 2016/9/6.
 */
public class EntranceConfigure {
    private String name;
    private int port;
    private Class<?> cls;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Class<?> getCls() {
        return cls;
    }

    public void setCls(Class<?> cls) {
        this.cls = cls;
    }
}
