package com.peony.demo.assembly.config;
import java.util.*;

import com.peony.demo.config.core.*;

import com.google.common.collect.*;

@Config("hero")
public class HeroContainer extends ConfigContainer<Integer, HeroConfig> {
	private List<HeroConfig> list = ImmutableList.of();

	private Map<Integer, HeroConfig> map = ImmutableMap.of();

	@Override
	public void init(List<HeroConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, HeroConfig> map = new LinkedHashMap<>();
		for(HeroConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<HeroConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, HeroConfig>  getMap() {
		return map;
	}

}