package com.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

import com.google.common.collect.*;

@Config("hardControlBuff")
public class HardControlBuffContainer extends ConfigContainer<Integer, HardControlBuffConfig> {
	private List<HardControlBuffConfig> list = ImmutableList.of();

	private Map<Integer, HardControlBuffConfig> map = ImmutableMap.of();

	@Override
	public void init(List<HardControlBuffConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, HardControlBuffConfig> map = new LinkedHashMap<>();
		for(HardControlBuffConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<HardControlBuffConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, HardControlBuffConfig>  getMap() {
		return map;
	}

}