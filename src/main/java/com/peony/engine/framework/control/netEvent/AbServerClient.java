package com.peony.engine.framework.control.netEvent;

import java.net.InetSocketAddress;

/**
 * Created by apple on 16-8-28.
 */
public abstract class AbServerClient implements ServerClient {
    protected int serverType;
    protected InetSocketAddress address;

    public int getServerType() {
        return serverType;
    }

    public void setServerType(int serverType) {
        this.serverType = serverType;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "ServerClient{" +
                "serverType=" + serverType +
                ", address=" + address +
                '}';
    }
}
