package com.peony.demo.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.demo.config.core.*;
import com.google.common.collect.*;

public class SkillConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * name
	 */
	private String name = null;
	/**
	 * effectIdListMap
	 */
	private ImmutableMap<Integer,ImmutableList<Integer>> effectIdListMap = ImmutableMap.copyOf(new LinkedHashMap<>());
	/**
	 * targetType
	 */
	private Tuple3<Integer,Integer,Integer> targetType = null;
	/**
	 * useMagic
	 */
	private Long useMagic = null;
	/**
	 * enableWhileDead
	 */
	private boolean enableWhileDead = false;
	/**
	 * triggerTime
	 */
	private Integer triggerTime = null;
	/**
	 * triggerTargetType
	 */
	private Tuple3<Integer,Integer,Integer> triggerTargetType = null;
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
	 * effectIdListMap
	 */
	public ImmutableMap<Integer,ImmutableList<Integer>> getEffectIdListMap() {
		return effectIdListMap;
	}

	/**
	 * targetType
	 */
	public Tuple3<Integer,Integer,Integer> getTargetType() {
		return targetType;
	}

	/**
	 * useMagic
	 */
	public Long getUseMagic() {
		return useMagic;
	}

	/**
	 * enableWhileDead
	 */
	public boolean isEnableWhileDead() {
		return enableWhileDead;
	}

	/**
	 * triggerTime
	 */
	public Integer getTriggerTime() {
		return triggerTime;
	}

	/**
	 * triggerTargetType
	 */
	public Tuple3<Integer,Integer,Integer> getTriggerTargetType() {
		return triggerTargetType;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("name", name);
		obj.put("effectIdListMap", effectIdListMap);
		obj.put("targetType", targetType);
		obj.put("useMagic", useMagic);
		obj.put("enableWhileDead", enableWhileDead);
		obj.put("triggerTime", triggerTime);
		obj.put("triggerTargetType", triggerTargetType);
		return obj;
	}


	// --------------------------------------extra--end
}