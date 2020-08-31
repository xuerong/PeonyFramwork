package com.peony.demo.assembly.config;
import java.util.*;

import com.peony.demo.config.core.*;

import com.google.common.collect.*;

@Config("addBuffEffect")
public class AddBuffEffectContainer extends ConfigContainer<Integer, AddBuffEffectConfig> {
	private List<AddBuffEffectConfig> list = ImmutableList.of();

	private Map<Integer, AddBuffEffectConfig> map = ImmutableMap.of();

	@Override
	public void init(List<AddBuffEffectConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, AddBuffEffectConfig> map = new LinkedHashMap<>();
		for(AddBuffEffectConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<AddBuffEffectConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, AddBuffEffectConfig>  getMap() {
		return map;
	}

}