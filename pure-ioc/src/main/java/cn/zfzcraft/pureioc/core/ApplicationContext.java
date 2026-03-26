package cn.zfzcraft.pureioc.core;

import java.lang.annotation.Annotation;
import java.util.List;


public interface ApplicationContext{

	Environment getEnvironment();
	
	<T> T getBean(Class<T> clazz);

	List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass);

	List<Class<?>> getImplementationClasses(Class<?> interfaceClass);

}
