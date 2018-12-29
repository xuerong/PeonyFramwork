package com.peony.engine.framework.security.exception;

import com.peony.engine.framework.tool.util.MessageFormatter;

/**
 * Created by a on 2016/8/24.
 * TODO 添加一个属性，用于判断是否关闭User连接
 */
public class MMException extends RuntimeException {
    private static final long serialVersionUID = 5908169566019016047L;
    private ExceptionType exceptionType = ExceptionType.Common;
    private String errMsg = null;

    public MMException(){
        super();
    }
    public MMException(String msg,Object... args) {
        super(msg);
        this.errMsg = MessageFormatter.arrayFormat(msg,args);
//        printStackTrace();
    }
    public MMException(ExceptionType exceptionType,String msg,Object... args) {
        super(msg);
        this.exceptionType = exceptionType;
        this.errMsg = MessageFormatter.arrayFormat(msg,args);
//        printStackTrace();
    }

    public MMException(String msg,Throwable cause) {
        super(msg, cause);
//        cause.printStackTrace();
    }

    public MMException(Throwable cause) {
        super(null, cause);
        cause.printStackTrace();
    }

    public void setMessage(String message){
        this.errMsg = message;
    }

    public String getMessage(){
        String tmp = this.errMsg;
        if (tmp==null && this.getCause()!=null){
            tmp = this.getCause().getMessage();
        }
        return tmp;
    }
    public ExceptionType getExceptionType(){
        return exceptionType;
    }
    public enum ExceptionType{
        Common,
        TxCommitFail, // 事务提交失败
        DataBaseFail, // 数据库提交失败
        SendNetEventFail, // 远程调用失败
        RemoteFail,
        StartUpFail, // 启动失败
    }
}
