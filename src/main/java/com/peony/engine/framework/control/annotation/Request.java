package com.peony.engine.framework.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2015/11/16.
 * 外部进来的访问
 * 任何一个opcode在整个应用中只有一个处理方法
 *
 * 方法的格式：
 * public RetPacket xxx(Object clientData, Session session)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Request {
    /**
     * opcode,框架根据该参数进行路径导航
     * TODO 这个可以考虑换成int类型
     * **/
    int opcode();
}
