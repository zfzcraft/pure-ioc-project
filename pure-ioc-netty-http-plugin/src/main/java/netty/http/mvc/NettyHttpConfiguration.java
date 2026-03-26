package netty.http.mvc;
import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.Configuration;
import cn.zfzcraft.pureioc.annotations.Eager;
import cn.zfzcraft.pureioc.annotations.Imports;
import cn.zfzcraft.pureioc.core.ApplicationContext;

@Configuration
@Imports(HttpProperties.class)
public class NettyHttpConfiguration {

	@Eager
	@Bean
	public NettyHttpServer nettyHttpServer(ApplicationContext applicationContext, HttpProperties httpProperties) throws InterruptedException {
		return new NettyHttpServer(applicationContext, httpProperties);
	}
}
