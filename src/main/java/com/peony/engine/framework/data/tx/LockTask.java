package com.peony.engine.framework.data.tx;

/**
 * Created by a on 2016/10/27.
 */
public interface LockTask<T> {
    public T run();
}
