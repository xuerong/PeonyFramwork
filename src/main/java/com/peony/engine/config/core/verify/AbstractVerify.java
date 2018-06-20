package com.peony.engine.config.core.verify;

import com.alibaba.fastjson.JSONArray;

/**
 *
 * Created by jiangmin.wu
 */
public abstract class AbstractVerify implements IVerify {

	protected String type;

	public AbstractVerify(String type) {
		this.type = type;
	}

	public abstract void init(JSONArray param);

	@Override
	public String getType() {
		return type;
	}
}
