package cn.zfzcraft.pureioc.core;

import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;

@ConfigurationProperties(prefix = "env")
public class EnvironmentProperties {

	private String active;

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	
	
}
