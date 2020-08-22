package com.assembly.config;
import java.util.*;

import com.alibaba.fastjson.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

import com.google.common.collect.*;

@Config("changeDataEffect")
public class ChangeDataEffectContainer extends ConfigContainer<Integer, ChangeDataEffectConfig> {
	private List<ChangeDataEffectConfig> list = ImmutableList.of();

	private Map<Integer, ChangeDataEffectConfig> map = ImmutableMap.of();

	@Override
	public void init(List<ChangeDataEffectConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, ChangeDataEffectConfig> map = new LinkedHashMap<>();
		for(ChangeDataEffectConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<ChangeDataEffectConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, ChangeDataEffectConfig>  getMap() {
		return map;
	}

}