package pure.ioc.plugin.hikaricp;

import java.util.Set;

import cn.zfzcraft.pureioc.core.spi.Plugin;

public class HikariPlugin implements Plugin {

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(HikariConfiguration.class);

	}

}
