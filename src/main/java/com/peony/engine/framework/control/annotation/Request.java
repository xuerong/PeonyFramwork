package com.peony.engine.framework.control.annotation;

import com.peony.engine.framework.control.request.RequestService;
import com.peony.engine.framework.data.entity.session.Session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 客户端请求处理器。
 * <p>
 * 当给一个方法添加了<code>Request</code>，该方法将被纳入客户端请求处理器，可以通过调用
 * {@link RequestService#handleRequest RequestService.handleRequest}实现对客户端
 * 请求处理器的调用。
 * <p>
 * 方法必须满足：1、两个参数，且第二个参数必须为{@link Session}，2、第一个参数和返回值，由
 * <code>RequestService.handleRequest</code>传入的参数和返回的参数决定，一般情况下，最
 * 终决定于该应用使用的通信协议。
 * 比如：
 * <pre>
 *     public Object requestHandler(Object req, Session session);
 * </pre>
 * <strong>同一个请求协议号只能有一个处理方法</strong><p>
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @author zhengyuzhen
 * @see RequestService
 * @see Service
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Request {
    /**
     * 请求协议号
     * @return
     */
    int opcode();
}
