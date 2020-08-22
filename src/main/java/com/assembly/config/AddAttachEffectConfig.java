package com.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

public class AddAttachEffectConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * name
	 */
	private String name = null;
	/**
	 * targetType
	 */
	private Tuple3<Integer,Integer,Integer> targetType = null;
	/**
	 * targetSkillId
	 */
	private Integer targetSkillId = null;
	/**
	 * targetEffectType
	 */
	private Integer targetEffectType = null;
	/**
	 * targetEffectId
	 */
	private Integer targetEffectId = null;
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
	 * targetType
	 */
	public Tuple3<Integer,Integer,Integer> getTargetType() {
		return targetType;
	}

	/**
	 * targetSkillId
	 */
	public Integer getTargetSkillId() {
		return targetSkillId;
	}

	/**
	 * targetEffectType
	 */
	public Integer getTargetEffectType() {
		return targetEffectType;
	}

	/**
	 * targetEffectId
	 */
	public Integer getTargetEffectId() {
		return targetEffectId;
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
		obj.put("targetType", targetType);
		obj.put("targetSkillId", targetSkillId);
		obj.put("targetEffectType", targetEffectType);
		obj.put("targetEffectId", targetEffectId);
		obj.put("attachEffectIdListMap", attachEffectIdListMap);
		return obj;
	}


	// --------------------------------------extra--end
}