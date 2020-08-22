package com.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

public class ChangeDataChangeEffectConfig extends AbstractConfig<Integer> {
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
	 * targetEffectId
	 */
	private Integer targetEffectId = null;
	/**
	 * newTargetType
	 */
	private Tuple3<Integer,Integer,Integer> newTargetType = null;
	/**
	 * changeType
	 */
	private Integer changeType = null;
	/**
	 * changeValue
	 */
	private Long changeValue = null;
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
	 * targetEffectId
	 */
	public Integer getTargetEffectId() {
		return targetEffectId;
	}

	/**
	 * newTargetType
	 */
	public Tuple3<Integer,Integer,Integer> getNewTargetType() {
		return newTargetType;
	}

	/**
	 * changeType
	 */
	public Integer getChangeType() {
		return changeType;
	}

	/**
	 * changeValue
	 */
	public Long getChangeValue() {
		return changeValue;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("name", name);
		obj.put("targetType", targetType);
		obj.put("targetSkillId", targetSkillId);
		obj.put("targetEffectId", targetEffectId);
		obj.put("newTargetType", newTargetType);
		obj.put("changeType", changeType);
		obj.put("changeValue", changeValue);
		return obj;
	}


	// --------------------------------------extra--end
}