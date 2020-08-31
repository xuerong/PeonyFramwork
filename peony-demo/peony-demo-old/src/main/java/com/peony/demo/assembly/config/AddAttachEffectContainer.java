package com.peony.demo.assembly.config;
import java.util.*;

import com.peony.demo.config.core.*;

import com.google.common.collect.*;

@Config("addAttachEffect")
public class AddAttachEffectContainer extends ConfigContainer<Integer, AddAttachEffectConfig> {
	private List<AddAttachEffectConfig> list = ImmutableList.of();

	private Map<Integer, AddAttachEffectConfig> map = ImmutableMap.of();

	@Override
	public void init(List<AddAttachEffectConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, AddAttachEffectConfig> map = new LinkedHashMap<>();
		for(AddAttachEffectConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<AddAttachEffectConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, AddAttachEffectConfig>  getMap() {
		return map;
	}

}