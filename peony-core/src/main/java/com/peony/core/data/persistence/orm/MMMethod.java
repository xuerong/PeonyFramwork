package com.peony.core.data.persistence.orm;

/**
 * @Author: zhengyuzhen
 * @Date: 2019-09-03 16:32
 */
public interface MMMethod {
    Object invoke(Object object,Object... args);
}
