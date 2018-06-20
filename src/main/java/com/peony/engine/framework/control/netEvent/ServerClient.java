package com.peony.engine.framework.control.netEvent;

/**
 * Created by apple on 16-8-14.
 * 本节点与其它节点的链接对象
 */
public interface ServerClient {

    boolean isConnected();

    /**
     * 在指定的超时时间之内连接远程服务器.
     *
     * 注意: 该方法即使是返回超时异常, 其内部也会继续尝试获得连接.
     *
     * @param timeoutSeconds
     */
    void connectSync(int timeoutSeconds);

    /**
     *
     */
    void connectAsync();

    Object request(Object msg);

    void push(Object msg);
}
