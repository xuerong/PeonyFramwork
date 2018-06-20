package com.peony.engine.framework.security.exception;

/**
 * Created by Administrator on 2015/11/13.
 */
public enum ExceptionLevel {
    Fatal,// 致命，需要马上重启服务器
    Serious,// 严重，需要重启服务器
    Error,// 错误，重启服务器或其他方式补偿
    Warn,// 提醒，小概率事件发生，不会导致错误，可能引起一定的性能等损失，
    Info// 信息

}
