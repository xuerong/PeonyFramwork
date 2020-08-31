package com.peony.demo.assembly.config;
import java.util.*;

import com.peony.demo.config.core.*;
import com.google.common.collect.*;

@Config("skill")
public class SkillContainer extends ConfigContainer<Integer, SkillConfig> {
	private List<SkillConfig> list = ImmutableList.of();

	private Map<Integer, SkillConfig> map = ImmutableMap.of();

	@Override
	public void init(List<SkillConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, SkillConfig> map = new LinkedHashMap<>();
		for(SkillConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<SkillConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, SkillConfig>  getMap() {
		return map;
	}

}