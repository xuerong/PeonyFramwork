package com.assembly.config;
import java.util.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

@Config("changeBasicDataType")
public class ChangeDataTypeContainer extends ConfigContainer<Integer, ChangeDataTypeConfig> {
	private List<ChangeDataTypeConfig> list = ImmutableList.of();

	private Map<Integer, ChangeDataTypeConfig> map = ImmutableMap.of();

	@Override
	public void init(List<ChangeDataTypeConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, ChangeDataTypeConfig> map = new LinkedHashMap<>();
		for(ChangeDataTypeConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<ChangeDataTypeConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, ChangeDataTypeConfig>  getMap() {
		return map;
	}

}