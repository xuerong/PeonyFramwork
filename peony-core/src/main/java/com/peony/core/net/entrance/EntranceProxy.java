package com.peony.core.net.entrance;

import com.peony.core.control.aop.AspectProxy;
import com.peony.core.control.aop.annotation.Aspect;
import com.peony.core.control.event.EventService;
import com.peony.core.server.SysConstantDefine;
import com.peony.core.control.BeanHelper;

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
        eventService.fireEvent(SysConstantDefine.Event_EntranceStart,object);
    }

    @Override
    public void exceptionCatch(Throwable e) {

    }
}
