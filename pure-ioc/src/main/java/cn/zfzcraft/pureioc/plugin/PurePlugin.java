package cn.zfzcraft.pureioc.plugin;

import java.util.Set;

import cn.zfzcraft.pureioc.core.matcher.CompomentBeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.matcher.ConfigurationBeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.matcher.ConfigurationMethodBeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.matcher.ConfigurationPropertiesBeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.spi.Plugin;

public class PurePlugin implements Plugin{

	@Override
	public void registerBeanClasses(Set<Class<?>> pluginClasses) {
		pluginClasses.add(CompomentBeanFactoryAnnotationMatcher.class);
		pluginClasses.add(ConfigurationBeanFactoryAnnotationMatcher.class);
		pluginClasses.add(ConfigurationMethodBeanFactoryAnnotationMatcher.class);
		pluginClasses.add(ConfigurationPropertiesBeanFactoryAnnotationMatcher.class);
		
	}

}
