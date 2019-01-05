package com.peony.engine.framework.control.annotation;

import com.peony.engine.framework.control.aop.annotation.AspectMark;
import com.peony.engine.framework.control.gm.Gm;
import com.peony.engine.framework.control.netEvent.remote.BroadcastRPC;
import com.peony.engine.framework.control.rpc.Remotable;
import com.peony.engine.framework.control.statistics.Statistics;
import com.peony.engine.framework.data.tx.Tx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务。
 * <p>
 * 用于标识一个类为服务类，服务类是peony中提供服务的基本单元。同时，只有被
 * <tt>Service</tt>标识的类才能被纳入peony的服务，peony容器便是Service容器，
 * 并处理在该类中添加的其它注解，包括：
 * <ul>
 *     <li>{@link Request}</li>
 *     <li>{@link EventListener}</li>
 *     <li>{@link NetEventListener}</li>
 *     <li>{@link Updatable}</li>
 *     <li>{@link Tx}</li>
 *     <li>{@link Remotable}</li>
 *     <li>{@link BroadcastRPC}</li>
 *     <li>{@link Gm}</li>
 *     <li>{@link Statistics}</li>
 *     <li>{@link AspectMark}</li>
 * </ul>
 * <p>
 *
 * 当在一个<tt>Service</tt>中引用其它<tt>Service</tt>，可以直接在该
 * <tt>Service</tt>中直接声明，系统在启动的时候会自动为其赋值。如果需要自己获取
 * <tt>Service</tt>对象，可以通过如下方法实现：
 * <pre>
 *     XxxService service = BeanHelper.getServiceBean(XxxService.class);
 * </pre>
 * <p>
 * 系统启动后，所有的<tt>Service</tt>是以单例的方式存在的，系统在实现容器功能时，
 * 可能会以继承的方式，实例化其子类，所以，<tt>Service</tt>不能是<tt>final</tt>
 * 的，同时系统已反射的方式创建<tt>Service</tt>对象，所以，<tt>Service</tt>
 * 的不能有带参数的构造函数。
 * <p>
 * <tt>Service</tt>的初始化行为不建议使用构造函数，而是<tt>Service</tt>提供的
 * <tt>init</tt>参数，在<tt>init</tt>参数对应的方法被执行时，所有的<tt>Service</tt>
 * 都已经创建，并且其声明的其它<tt>Service</tt>都已经赋值。由于在服务初始化过程中会有相
 * 互依赖，为防止被依赖的<tt>Service</tt>还没有初始化，可以使用<tt>initPriority</tt>
 * 参数来定义初始化优先级，同理，在系统关闭时，也有对应的<tt>destroy</tt>和
 * <tt>destroyPriority</tt>。
 * <p>
 * 如果在执行初始化方法时，抛出了异常，系统会捕获这个异常，并继续初始化下一个服务，而不是
 * 停止系统的运行。如果你的服务初始化失败时必须关闭系统，请自行捕获异常，并关闭系统。
 *
 * @author zhengyuzhen
 * @see com.peony.engine.framework.control.ServiceHelper
 * @see com.peony.engine.framework.tool.helper.BeanHelper
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    /**
     * 初始化方法，值为初始化方法的名字，方法不接受参数，且返回void。方法会在系统启动时
     * 调用。
     * @return
     */
    String init() default "";

    /**
     * 初始化的优先级,越小越先初始化。
     * <p>
     * 通过设置初始化的优先级，可以控制在某个服务初始化时，其引用的服务已经初始化完成。
     * 但这并不能彻底解决服务之间循环引用的问题。不过，如果初始化时所引用的服务可以不要求已经
     * 初始化，也就没有问题的，否则，需要由程序来解决相应问题。
     * @return
     */
    int initPriority() default 10;

    /**
     * 销毁方法，值为销毁方法的名字，方法不接受参数，且返回void。方法会在系统关闭时调用
     * @return
     */
    String destroy() default "";

    /**
     * 销毁的优先级,越小越先初始化。
     * @return
     */
    int destroyPriority() default 10;
}
