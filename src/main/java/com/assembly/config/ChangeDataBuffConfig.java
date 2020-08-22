package com.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

public class ChangeDataBuffConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * name
	 */
	private String name = null;
	/**
	 * basicDataType
	 */
	private Integer basicDataType = null;
	/**
	 * valueTypeId
	 */
	private Integer valueTypeId = null;
	/**
	 * value
	 */
	private Integer value = null;
	/**
	 * calWhenUse
	 */
	private boolean calWhenUse = false;
	/**
	 * round
	 */
	private Integer round = null;
	/**
	 * consumptive
	 */
	private boolean consumptive = false;
	/**
	 * canCrit
	 */
	private boolean canCrit = false;
	/**
	 * canDefend
	 */
	private boolean canDefend = false;
	/**
	 * buffTag
	 */
	private ImmutableList<Integer> buffTag = ImmutableList.copyOf(new ArrayList<>());
	/**
	 * id
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * name
	 */
	public String getName() {
		return name;
	}

	/**
	 * basicDataType
	 */
	public Integer getBasicDataType() {
		return basicDataType;
	}

	/**
	 * valueTypeId
	 */
	public Integer getValueTypeId() {
		return valueTypeId;
	}

	/**
	 * value
	 */
	public Integer getValue() {
		return value;
	}

	/**
	 * calWhenUse
	 */
	public boolean isCalWhenUse() {
		return calWhenUse;
	}

	/**
	 * round
	 */
	public Integer getRound() {
		return round;
	}

	/**
	 * consumptive
	 */
	public boolean isConsumptive() {
		return consumptive;
	}

	/**
	 * canCrit
	 */
	public boolean isCanCrit() {
		return canCrit;
	}

	/**
	 * canDefend
	 */
	public boolean isCanDefend() {
		return canDefend;
	}

	/**
	 * buffTag
	 */
	public ImmutableList<Integer> getBuffTag() {
		return buffTag;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("name", name);
		obj.put("basicDataType", basicDataType);
		obj.put("valueTypeId", valueTypeId);
		obj.put("value", value);
		obj.put("calWhenUse", calWhenUse);
		obj.put("round", round);
		obj.put("consumptive", consumptive);
		obj.put("canCrit", canCrit);
		obj.put("canDefend", canDefend);
		return obj;
	}


	// --------------------------------------extra--end
}