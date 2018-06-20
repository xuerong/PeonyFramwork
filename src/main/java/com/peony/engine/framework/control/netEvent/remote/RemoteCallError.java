package com.peony.engine.framework.control.netEvent.remote;

public class RemoteCallError {
    private String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString(){
        return msg;
    }

}
