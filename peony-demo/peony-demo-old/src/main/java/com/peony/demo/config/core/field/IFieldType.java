package com.peony.demo.config.core.field;

import com.peony.demo.config.core.ConfigContainer;
import com.peony.demo.config.core.ConfigFieldMeta;
import com.peony.demo.config.core.IConfig;
import com.peony.demo.config.core.verify.VerifyContext;

/**
 * Created by jiangmin.wu on 2018/3/6.
 */
public interface IFieldType<T> {
    /**
     * 配置文件标注的类型
     *
     * @return
     */
    String getName();

    /**
     * 当前列对应的 java 对象名称
     *
     * @return
     */
    String getJavaType();

    /**
     * 默认值
     *
     * @return
     */
    String getDefaultVal();

    /**
     * 将配置的值转化为对应的 java 对象.
     *
     * @param rawVal
     * @return
     */
    T parseValue(String rawVal);

    /**
     * 验证初始化, 生成验证实例和相应的辅助数据
     * <p>
     * 如: unique 初始化 Set 等
     *
     * @param fieldMeta
     */
    void beforeVerify(ConfigFieldMeta fieldMeta) throws Exception;

    /**
     * 验证
     * <p>
     * 这里抽离出验证方法, 是为了不同数据结构复用一套验证实现类.
     *
     * @param context
     * @param configItem
     * @param fieldMeta
     * @param selfContainer
     * @param value
     * @param exception
     */
    String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta fieldMeta, T value, ConfigContainer selfContainer, boolean exception) throws Exception;
}
