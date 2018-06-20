package com.peony.engine.framework.net.packet;

/**
 * Created by a on 2016/9/19.
 */
public class NettyPBPacket {
    private int id = -1; // 包的标识符，用于识别该包，以确定返回的就是发出的消息，-1标识没有标识，如推送消息
    private int opcode;
    private byte[] data;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOpcode() {
        return opcode;
    }

    public void setOpcode(int opcode) {
        this.opcode = opcode;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
