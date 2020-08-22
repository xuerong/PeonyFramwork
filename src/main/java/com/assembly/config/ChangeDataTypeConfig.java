package com.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

public class ChangeDataTypeConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * absolute
	 */
	private boolean absolute = false;
	/**
	 * from
	 */
	private boolean from = false;
	/**
	 * basicDataType
	 */
	private Integer basicDataType = null;
	/**
	 * id
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * absolute
	 */
	public boolean isAbsolute() {
		return absolute;
	}

	/**
	 * from
	 */
	public boolean isFrom() {
		return from;
	}

	/**
	 * basicDataType
	 */
	public Integer getBasicDataType() {
		return basicDataType;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("absolute", absolute);
		obj.put("from", from);
		obj.put("basicDataType", basicDataType);
		return obj;
	}


	// --------------------------------------extra--end

}