package com.peony.engine.config.core.verify;

import com.peony.engine.config.core.ConfigFieldMeta;
import com.peony.engine.config.core.IConfig;
import com.peony.engine.config.core.ConfigContainer;

/**
 * 配置值验证抽象
 *
 * Created by jiangmin.wu
 */
public interface IVerify {

    String getType();

    String desc();

    /**
     * 不关心具体数据类型的验证实体, 这里只负责对单一值得验证
     * <p>
     * 像map,array,tupleX 等数据结构由 FieldType 层负责解析
     *
     * @param context
     * @param configItem
     * @param field
     * @param value
     * @param selfContainer
     * @param exception
     * @return  null 为验证通过, 字符串会描述详细的不通过原因和对应的字段 通用模式为: 验证类型中文描述 xmlName fieldName id [partKey] value
     * @throws Exception 异常, 描述同 return
     */
    String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta field, Object value, ConfigContainer selfContainer, boolean exception) throws Exception;
}
