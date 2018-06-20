package com.peony.engine.config.core.verify.impl;

import com.alibaba.fastjson.JSONArray;
import com.peony.engine.config.core.*;
import com.peony.engine.config.core.verify.AbstractVerify;
import com.peony.engine.config.core.verify.VerifyContext;
import com.peony.engine.config.core.ConfigException;

/**
 * {t:'range',p:[min,max]}
 * @author wjm
 */
public class RangeVerify extends AbstractVerify {

	private double min;
	private double max;

	public RangeVerify() {
		super("range");
	}

	@Override
	public void init(JSONArray param) {
		min = param.getDoubleValue(0);
		max = param.getDoubleValue(1);
	}

	@Override
	public String desc() {
		return "数值范围验证: {t:'range', p:[min, max]}";
	}

	@Override
	public String verify(VerifyContext context, IConfig<?> configItem, ConfigFieldMeta cfgField, Object val,
						 ConfigContainer selfContainer, boolean exception) throws Exception {
		if (val == null) {
			return null;
		}

		Double dv = Double.valueOf(val.toString());
		if (dv > max || dv < min) {
			String error = String.format("范围不合法([%s, %s]) %s %s %s %s",
					min, max, selfContainer.getMetaData().getFileName(), cfgField.getName(), configItem.getId(), val);
			if (exception) {
				throw new ConfigException(error);
			} else {
				return error;
			}
		}
		return null;
	}
}
