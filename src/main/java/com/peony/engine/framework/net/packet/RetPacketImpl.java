package com.peony.engine.framework.net.packet;

/**
 * Created by Administrator on 2015/11/19.
 */
public class RetPacketImpl implements RetPacket {
    private int opcode;
    private boolean keepSession;
    private Object retData;
    /**
     * 默认是保存session
     * **/
    public RetPacketImpl(int opcode,Object retData){
        this.opcode=opcode;
        this.keepSession=true;
        this.retData=retData;
    }
    public RetPacketImpl(int opcode,boolean keepSession,Object retData){
        this.opcode=opcode;
        this.keepSession=keepSession;
        this.retData=retData;
    }
    @Override
    public int getOpcode() {
        return opcode;
    }

    @Override
    public boolean keepSession() {
        return keepSession;
    }

    @Override
    public Object getRetData() {
        return retData;
    }
}
