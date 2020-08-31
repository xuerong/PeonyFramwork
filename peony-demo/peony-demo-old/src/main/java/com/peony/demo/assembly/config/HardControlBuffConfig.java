package com.peony.demo.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.demo.config.core.*;
import com.google.common.collect.*;

public class HardControlBuffConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * name
	 */
	private String name = null;
	/**
	 * round
	 */
	private Integer round = null;
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
	 * round
	 */
	public Integer getRound() {
		return round;
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
		obj.put("round", round);
		return obj;
	}


	// --------------------------------------extra--end
}