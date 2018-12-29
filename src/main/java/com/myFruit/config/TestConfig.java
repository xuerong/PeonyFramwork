package com.myFruit.config;

import com.alibaba.fastjson.JSONObject;
import com.peony.engine.config.core.AbstractConfig;

public class TestConfig extends AbstractConfig<Integer> {
	/**
	 * id
	 */
	private Integer id = null;
	/**
	 * level
	 */
	private Integer level = null;
	/**
	 * need_friends
	 */
	private Integer need_friends = null;
	/**
	 * animation
	 */
	private String animation = null;
	/**
	 * max_power
	 */
	private Integer max_power = null;
	/**
	 * tap_reward
	 */
	private Integer tap_reward = null;
	/**
	 * sp_reward
	 */
	private String sp_reward = null;
	/**
	 * id
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * level
	 */
	public Integer getLevel() {
		return level;
	}

	/**
	 * need_friends
	 */
	public Integer getNeedFriends() {
		return need_friends;
	}

	/**
	 * animation
	 */
	public String getAnimation() {
		return animation;
	}

	/**
	 * max_power
	 */
	public Integer getMaxPower() {
		return max_power;
	}

	/**
	 * tap_reward
	 */
	public Integer getTapReward() {
		return tap_reward;
	}

	/**
	 * sp_reward
	 */
	public String getSpReward() {
		return sp_reward;
	}



	// --------------------------------------extra--begin

	public JSONObject toJsonObj() {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("level", level);
		obj.put("need_friends", need_friends);
		obj.put("animation", animation);
		obj.put("max_power", max_power);
		obj.put("tap_reward", tap_reward);
		obj.put("sp_reward", sp_reward);
		return obj;
	}


	// --------------------------------------extra--end

}