package pure.ioc.plugin.mybatisplus;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.Environment.Builder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;

import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.ConditionalOnMissingBean;
import cn.zfzcraft.pureioc.annotations.Configuration;
@Configuration
public class MybatisPlusConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public SqlSessionFactory sessionFactory(DataSource dataSource) {
		Environment environment = new Builder("master").dataSource(dataSource).transactionFactory(new JdbcTransactionFactory()).build();
		MybatisConfiguration config = new MybatisConfiguration(environment);
		return new MybatisSqlSessionFactoryBuilder().build(config);

	}
}
