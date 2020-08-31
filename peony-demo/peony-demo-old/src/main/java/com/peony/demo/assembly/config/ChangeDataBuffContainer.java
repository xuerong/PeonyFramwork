package com.peony.demo.assembly.config;
import java.util.*;

import com.peony.demo.config.core.*;

import com.google.common.collect.*;

@Config("changeDataBuff")
public class ChangeDataBuffContainer extends ConfigContainer<Integer, ChangeDataBuffConfig> {
	private List<ChangeDataBuffConfig> list = ImmutableList.of();

	private Map<Integer, ChangeDataBuffConfig> map = ImmutableMap.of();

	@Override
	public void init(List<ChangeDataBuffConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, ChangeDataBuffConfig> map = new LinkedHashMap<>();
		for(ChangeDataBuffConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<ChangeDataBuffConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, ChangeDataBuffConfig>  getMap() {
		return map;
	}

}