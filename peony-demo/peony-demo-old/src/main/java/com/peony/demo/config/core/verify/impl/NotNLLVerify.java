package com.peony.demo.config.core.verify.impl;

import com.alibaba.fastjson.JSONArray;
import com.peony.demo.config.core.ConfigException;
import com.peony.demo.config.core.verify.AbstractVerify;
import com.peony.demo.config.core.verify.VerifyContext;
import org.apache.commons.lang3.StringUtils;

import com.peony.demo.config.core.ConfigContainer;
import com.peony.demo.config.core.ConfigFieldMeta;
import com.peony.demo.config.core.IConfig;

/**
 * @author wjm
 */
public class NotNLLVerify extends AbstractVerify {

	public NotNLLVerify() {
		super("notnull");
	}

	@Override
	public void init(JSONArray param) {

	}

	@Override
	public String desc() {
		return "非空验证: {t:'notnull'}";
	}

	@Override
	public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta cfgField, Object val,
						 ConfigContainer selfContainer, boolean exception) throws Exception {

		if (val == null || StringUtils.isBlank(val.toString())) {
			String error = String.format("字段不能为空 %s %s %s",
					selfContainer.getMetaData().getFileName(), cfgField.getName(), configItem.getId(), val);
			if (exception) {
				throw new ConfigException(error);
			} else {
				return error;
			}
		}

		return null;
	}
}
