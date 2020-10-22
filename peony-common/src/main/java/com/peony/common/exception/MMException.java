package com.peony.common.exception;

import com.peony.common.tool.util.MessageFormatter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

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
        this(null, msg, args);
    }
    public MMException(ExceptionType exceptionType,String msg,Object... args) {
        this.exceptionType = exceptionType;
        if(ArrayUtils.isNotEmpty(args)){
            if(args[args.length - 1].getClass().isAssignableFrom(Throwable.class)){
                initCause((Throwable)args[args.length - 1]);
                args = Arrays.copyOf(args, args.length - 1);
            }
        }
        this.errMsg = MessageFormatter.arrayFormat(msg,args);
    }

    public MMException(String msg,Throwable cause) {
        this(null, msg, new Object[]{cause});
    }

    public MMException(Throwable cause) {
        this(null, cause);
    }

    @Override
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
