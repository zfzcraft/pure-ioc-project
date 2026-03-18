package cn.zfzcraft.pureioc.core;

import java.util.List;
import java.util.Set;

import cn.zfzcraft.pureioc.core.compoments.FrameworkCompoment;


public interface ApplicationContext extends FrameworkCompoment{

	Environment getEnvironment();
	
	<T> T getBean(Class<T> clazz);

	<T> List<T> getBeansOfType(Class<T> interfaceClass);
	
	Set<Class<?>> getBeanClasses();

}
