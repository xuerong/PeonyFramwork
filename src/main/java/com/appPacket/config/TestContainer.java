package com.appPacket.config;
import java.util.*;

import com.peony.engine.config.core.*;
import com.google.common.collect.*;

@Config("test")
public class TestContainer extends ConfigContainer<Integer, TestConfig> {
	private List<TestConfig> list = ImmutableList.of();

	private Map<Integer, TestConfig> map = ImmutableMap.of();

	@Override
	public void init(List<TestConfig> list) {
		this.list = ImmutableList.copyOf(list);
		Map<Integer, TestConfig> map = new LinkedHashMap<>();
		for(TestConfig t:this.list) {
			map.put(t.getId(), t);
		}
		this.map = ImmutableMap.copyOf(map);
	}

	@Override
	public List<TestConfig>  getAll() {
		return list;
	}

	@Override
	public Map<Integer, TestConfig>  getMap() {
		return map;
	}

}