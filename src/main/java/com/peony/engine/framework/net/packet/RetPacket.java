package com.peony.engine.framework.net.packet;

/**
 * Created by Administrator on 2015/11/19.
 *
 * 这是Entrance的返回值，也是注解Request的返回值
 */
public interface RetPacket {
    public int getOpcode();
    public boolean keepSession();
    public Object getRetData();
}
