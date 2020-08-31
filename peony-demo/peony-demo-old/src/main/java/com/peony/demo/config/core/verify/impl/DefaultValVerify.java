package com.peony.demo.config.core.verify.impl;

import com.alibaba.fastjson.JSONArray;
import com.peony.demo.config.core.ConfigContainer;
import com.peony.demo.config.core.ConfigFieldMeta;
import com.peony.demo.config.core.IConfig;
import com.peony.demo.config.core.verify.AbstractVerify;
import com.peony.demo.config.core.verify.VerifyContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * @author wjm
 */
public class DefaultValVerify extends AbstractVerify {

	private String defaultValStr;

	public DefaultValVerify() {
		super("def");
	}

	@Override
	public String desc() {
		return "设置默认值: {t:'def',p:['默认值']}";
	}

	@Override
	public void init(JSONArray param) {
		defaultValStr = param.getString(0);
	}

	@Override
	public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta cfgField, Object val,
						 ConfigContainer selfContainer, boolean exception) throws Exception {

		if (val == null || StringUtils.isBlank(val.toString())) {
			Field itemField = configItem.getClass().getDeclaredField(cfgField.getName());
			itemField.setAccessible(true);
			Object defaultVal = null;
			switch (cfgField.getType()) {
				case "boolean": defaultVal = Boolean.valueOf(defaultValStr); break;
				case "byte": defaultVal = Byte.valueOf(defaultValStr); break;
				case "short": defaultVal = Short.valueOf(defaultValStr); break;
				case "int": defaultVal = Integer.valueOf(defaultValStr); break;
				case "long": defaultVal = Long.valueOf(defaultValStr); break;
				case "float": defaultVal = Float.valueOf(defaultValStr); break;
				case "double": defaultVal = Double.valueOf(defaultValStr); break;
			}
			itemField.set(configItem, defaultVal);
		}

		return null;
	}
}
