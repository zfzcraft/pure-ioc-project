package pure.ioc.plugin.netty.http;

import java.util.Set;

import cn.zfzcraft.pureioc.core.spi.Plugin;
import netty.http.mvc.HttpAnnotationFactoryBeanMatcher;
import netty.http.mvc.HttpProperties;
import netty.http.mvc.NettyHttpServer;

public class NettyHttpPlugin implements Plugin{

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(NettyHttpServer.class);
		classes.add(HttpAnnotationFactoryBeanMatcher.class);
		classes.add(HttpProperties.class);
	}

}
