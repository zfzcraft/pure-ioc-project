package cn.zfzcraft.pureioc.core;

import java.util.Map;

import cn.zfzcraft.pureioc.utils.NestedMapUtils;

public class LocalEnvironment implements Environment {
	
	private Map<String, Object> env;

	public LocalEnvironment(Map<String, Object> env) {
		super();
		this.env = env;
	}

	@Override
	public Object getProperty(String key) {
		return NestedMapUtils.getNestedValue(env, key);
	}

	@Override
	public <T> T getProperty(String prefix, Class<T> type) {
		return NestedMapUtils.loadAs(env, prefix, type);
	}

}
