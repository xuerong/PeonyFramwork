package com.peony.engine.framework.net.entrance;

import com.peony.engine.framework.control.aop.AspectProxy;
import com.peony.engine.framework.control.aop.annotation.Aspect;
import com.peony.engine.framework.control.event.EventService;
import com.peony.engine.framework.server.SysConstantDefine;
import com.peony.engine.framework.tool.helper.BeanHelper;

import java.lang.reflect.Method;

/**
 * Created by a on 2016/9/6.
 */
@Aspect(
        mark = {"EntranceStart"}
)
public class EntranceProxy extends AspectProxy {

    @Override
    public void before(Object object,Class<?> cls, Method method, Object[] params) {
    }

    @Override
    public void after(Object object,Class<?> cls, Method method, Object[] params, Object result) {
        EventService eventService = BeanHelper.getServiceBean(EventService.class);
        eventService.fireEvent(object,SysConstantDefine.Event_EntranceStart);
    }

    @Override
    public void exceptionCatch(Throwable e) {

    }
}
