package com.peony.demo.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.demo.config.core.*;
import com.google.common.collect.*;

public class AddBuffEffectConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * name
	 */
	private String name = null;
	/**
	 * buffType
	 */
	private Integer buffType = null;
	/**
	 * targetType
	 */
	private Tuple3<Integer,Integer,Integer> targetType = null;
	/**
	 * buffIds
	 */
	private ImmutableList<Integer> buffIds = ImmutableList.copyOf(new ArrayList<>());
	/**
	 * rate
	 */
	private Integer rate = null;
	/**
	 * canDodged
	 */
	private boolean canDodged = false;
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
	 * buffType
	 */
	public Integer getBuffType() {
		return buffType;
	}

	/**
	 * targetType
	 */
	public Tuple3<Integer,Integer,Integer> getTargetType() {
		return targetType;
	}

	/**
	 * buffIds
	 */
	public ImmutableList<Integer> getBuffIds() {
		return buffIds;
	}

	/**
	 * rate
	 */
	public Integer getRate() {
		return rate;
	}

	/**
	 * canDodged
	 */
	public boolean isCanDodged() {
		return canDodged;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("name", name);
		obj.put("buffType", buffType);
		obj.put("targetType", targetType);
		obj.put("buffIds", buffIds);
		obj.put("rate", rate);
		obj.put("canDodged", canDodged);
		return obj;
	}


	// --------------------------------------extra--end
}