package pure.ioc.plugin.hikaricp;

import com.zaxxer.hikari.HikariConfig;
import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;

@ConfigurationProperties(prefix = "datasource.hikari")
public class HikariProperties extends HikariConfig {

	
	private String jdbcUrl;
	
	private String username;
	
	private String password;
	
	private String driverClassName;
	
	public String getJdbcUrl() {
		return jdbcUrl;
	}
	public void setJdbcUrl(String jdbcUrl) {
		super.setJdbcUrl(jdbcUrl);
		this.jdbcUrl = jdbcUrl;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		super.setUsername(username);
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		super.setPassword(password);
		this.password = password;
	}
	public String getDriverClassName() {
		return driverClassName;
	}
	public void setDriverClassName(String driverClassName) {
		super.setDriverClassName(driverClassName);
		this.driverClassName = driverClassName;
	}
	

}
