package com.peony.engine.framework.control.gm;

import com.peony.engine.framework.control.annotation.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GM工具注解。
 * <p>
 * 对Service中的一个方法添加<tt>Gm</tt>注解，该方法将会纳入GM工具中，通过GM工具，可以方便的
 * 从外部调用该方法。GM工具是是一个网页工具，默认本地启动时，可以通过http://localhost:9801/gm
 * 访问。该工具的启用和端口的配置在mmserver.properties的entrance.gm.xxx字段。
 * <p>
 * GM方法支持三种返回值，分别是:{@code void}，{@code Map<String,String>}和{@code String}。
 * 而参数必须是基本类型或者{@code String}类型，数量任意。
 * <p>
 * <strong>该注解对应方法由系统加载，必须在{@link Service}中声明</strong>
 *
 * @author zhengyuzhen
 * @see GmService
 * @see GmAdmin
 * @see GmFilter
 * @see GmServlet
 * @see GmSegment
 * @see Service
 * @since 1.0
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Gm {
    /**
     * gm的id，系统唯一的。在GM工具中，也是gm列表的值。
     *
     * @return
     */
    String id();

    /**
     * 可选字段，用于说明该Gm的使用说明，或者作用。
     * @return
     */
    String describe() default "";

    /**
     * 参数的名字列表,gm方法的参数的名字，在GM工具中，会显示在gm界面的参数之前，
     * 不配置则显示param
     *
     * @return
     */
    String[] paramsName() default {};
}
