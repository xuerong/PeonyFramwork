package com.peony.engine.framework.control.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2015/11/16.
 * Service用来提供服务，包括：
 * 用户请求服务@Request
 * 监听事件服务@EventListener
 * 更新服务@Updatable
 * 监听远程调用服务@NetEventListener
 *
 * TODO 添加runOnEveryServer，改为singleService吧，包括update的这个属性
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    /**
     * 初始化方法
     * @return
     */
    String init() default "";
    int initPriority() default 10; // 初始化的优先级,越小越先初始化,

    /**
     * 销毁方法
     * @return
     */
    String destroy() default "";
    int destroyPriority() default 10; // 销毁的优先级,越小越先销毁,
    /**
     *
     */
    boolean runOnEveryServer() default true; // TODO 这个需要去掉，或者换成配置在哪个服务器运行，要根据这个编写service配置文件的实现
}
