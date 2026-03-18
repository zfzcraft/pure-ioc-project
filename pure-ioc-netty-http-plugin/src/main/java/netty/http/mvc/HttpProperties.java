package netty.http.mvc;

import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;

@ConfigurationProperties(prefix = "netty")
public class HttpProperties {

	private int port;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
