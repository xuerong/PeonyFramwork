package com.peony.engine.framework.control.event;

/**
 * Created by Administrator on 2015/11/18.
 * TODO short改为int
 */
public class EventData {
    private short event;
    private Object data;

    public EventData(short event){
        this.event=event;
    }
    public EventData(int event){
        this.event=(short) event;
    }

    public short getEvent(){
        return event;
    }

    public void setEvent(short event) {
        this.event = event;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
