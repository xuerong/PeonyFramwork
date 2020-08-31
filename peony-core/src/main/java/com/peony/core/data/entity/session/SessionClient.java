package com.peony.core.data.entity.session;

import java.io.Serializable;

/**
 * Created by Administrator on 2015/12/30.
 * session中的客户端实例接口
 */
public interface SessionClient extends Serializable {
    /**
     * 当session销毁的时候，调用它来处理SessionClient销毁之前的事情，
     * 不要做花时间较多的事情，否则建议采用异步形式
     * **/
    public void destroySession();
}
