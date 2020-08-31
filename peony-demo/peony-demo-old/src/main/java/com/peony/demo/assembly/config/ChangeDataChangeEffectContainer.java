package com.peony.demo.assembly.config;
import java.util.*;

import com.peony.demo.config.core.*;

import com.google.common.collect.*;

@Config("changeDataChangeEffect")
public class ChangeDataChangeEffectContainer extends ConfigContainer<Integer, ChangeDataChangeEffectConfig> {
	private List<ChangeDataChangeEffectConfig> list = ImmutableList.of();

	private Map<Integer, ChangeDataChangeEffectConfig> map = ImmutableMap.of();

	@Override
	public void init(List<ChangeDataChangeEffectConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, ChangeDataChangeEffectConfig> map = new LinkedHashMap<>();
		for(ChangeDataChangeEffectConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<ChangeDataChangeEffectConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, ChangeDataChangeEffectConfig>  getMap() {
		return map;
	}

}