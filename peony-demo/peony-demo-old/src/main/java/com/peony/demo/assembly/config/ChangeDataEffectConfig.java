package com.peony.demo.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.demo.config.core.*;
import com.google.common.collect.*;

public class ChangeDataEffectConfig extends AbstractConfig<Integer> {
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
	 * targetType
	 */
	private Tuple3<Integer,Integer,Integer> targetType = null;
	/**
	 * valueTypeId
	 */
	private Integer valueTypeId = null;
	/**
	 * value
	 */
	private Integer value = null;
	/**
	 * canDodged
	 */
	private boolean canDodged = false;
	/**
	 * canCrit
	 */
	private boolean canCrit = false;
	/**
	 * canDefend
	 */
	private boolean canDefend = false;
	/**
	 * attachEffectIdListMap
	 */
	private ImmutableMap<Integer,ImmutableList<Integer>> attachEffectIdListMap = ImmutableMap.copyOf(new LinkedHashMap<>());
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
	 * targetType
	 */
	public Tuple3<Integer,Integer,Integer> getTargetType() {
		return targetType;
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
	 * canDodged
	 */
	public boolean isCanDodged() {
		return canDodged;
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
	 * attachEffectIdListMap
	 */
	public ImmutableMap<Integer,ImmutableList<Integer>> getAttachEffectIdListMap() {
		return attachEffectIdListMap;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("name", name);
		obj.put("basicDataType", basicDataType);
		obj.put("targetType", targetType);
		obj.put("valueTypeId", valueTypeId);
		obj.put("value", value);
		obj.put("canDodged", canDodged);
		obj.put("canCrit", canCrit);
		obj.put("canDefend", canDefend);
		obj.put("attachEffectIdListMap", attachEffectIdListMap);
		return obj;
	}


	// --------------------------------------extra--end
}