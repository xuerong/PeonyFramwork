package com.peony.engine.framework.security.exception;

import com.peony.engine.framework.tool.util.MessageFormatter;

/**
 * Created by apple on 16-10-2.
 * 发往前端的异常,一般可用于弹框提示错误
 */
public class ToClientException extends RuntimeException {
    private int errCode = -10001;// TODO 异常代号:注意，这里不是访问号啊，后面要加一个
    private int opcode;
    private String errMsg = null;

    public ToClientException(){
        super();
    }
    public ToClientException(int errCode){
        super();
        this.errCode = errCode;
    }

    public ToClientException(String msg,Object... args) {
        super(msg);
        this.errMsg = MessageFormatter.arrayFormat(msg,args);
    }

    public ToClientException(Throwable cause) {
        super(null, cause);
    }

    public ToClientException(int errCode, String msg,Object... args) {
        super(msg);
        this.errCode = errCode;
        this.errMsg = MessageFormatter.arrayFormat(msg,args);
    }

    public ToClientException(int errCode, Throwable cause) {
        super(null, cause);
        this.errCode = errCode;
    }

    public void setMessage(String message){
        this.errMsg = message;
    }
    public int getErrCode() {
        return errCode;
    }

    public String getMessage(){
        String tmp = this.errMsg;
        if (tmp==null && this.getCause()!=null){
            tmp = this.getCause().getMessage();
        }
        return tmp;
    }

    public static ToClientException parseFromParams(Object... params){
        ToClientException toClientException = new ToClientException();
        toClientException.errCode = ((int)params[0]);
        toClientException.opcode = ((Integer)params[1]).intValue();
        toClientException.errMsg = (String)params[2];
        return toClientException;
    }
    public Object[] toParams(){
        Object[] params = new Object[3];
        params[0] = errCode;
        params[1] = opcode;
        params[2] = errMsg;
        return params;
    }
}
