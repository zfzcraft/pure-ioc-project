package pure.ioc.plugin.hikaricp;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.ConditionalOnMissingBean;
import cn.zfzcraft.pureioc.annotations.Configuration;
import cn.zfzcraft.pureioc.annotations.Imports;

@Configuration
@Imports(HikariProperties.class)
public class HikariConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public DataSource dataSource(HikariProperties properties) {
		HikariDataSource ds = new HikariDataSource(properties);
		return ds;
	}
}
