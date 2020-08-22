package com.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

public class HeroConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * name
	 */
	private String name = null;
	/**
	 * skillIdList
	 */
	private ImmutableList<Integer> skillIdList = ImmutableList.copyOf(new ArrayList<>());
	/**
	 * basicData
	 */
	private ImmutableMap<Integer,Long> basicData = ImmutableMap.copyOf(new LinkedHashMap<>());
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
	 * skillIdList
	 */
	public ImmutableList<Integer> getSkillIdList() {
		return skillIdList;
	}

	/**
	 * basicData
	 */
	public ImmutableMap<Integer,Long> getBasicData() {
		return basicData;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("name", name);
		obj.put("skillIdList", skillIdList);
		obj.put("basicData", basicData);
		return obj;
	}


	// --------------------------------------extra--end
}