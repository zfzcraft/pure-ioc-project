package pure.ioc.plugin.mybatisplus;

import java.util.Set;

import cn.zfzcraft.pureioc.core.spi.Plugin;

public class MybatisPlusPlugin implements Plugin {

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(MybatisPlusConfiguration.class);
		classes.add(MapperBeanFactory.class);
		classes.add(MybatisPlusBeanFactoryAnnotationMatcher.class);
	}

}
