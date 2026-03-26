package pure.ioc.plugin.netty.http;

import java.util.Set;

import cn.zfzcraft.pureioc.core.spi.Plugin;
import netty.http.mvc.HttpAnnotationFactoryBeanMatcher;
import netty.http.mvc.NettyHttpConfiguration;

public class NettyHttpPlugin implements Plugin{

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(NettyHttpConfiguration.class);
		classes.add(HttpAnnotationFactoryBeanMatcher.class);
	}

}
