package cn.zfzcraft.pureioc.core.factory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.exception.BeanCreationFailedException;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;


public class ConfigurationMethodBeanFactory implements BeanFactory{

	@Override
	public Object createBean(ApplicationContext applicationContext, AnnotatedElement beanElement) {
		Method beanMethod = (Method) beanElement;
	try {
		Class<?> configurationClass = beanMethod.getDeclaringClass();
		Object configurationInstance = applicationContext.getBean(configurationClass);
		Class<?>[] types = beanMethod.getParameterTypes();
		Object[] methodArgs = resolveArgs(applicationContext,types);
		beanMethod.setAccessible(true);
		Object	instance = beanMethod.invoke(configurationInstance, methodArgs);
		return instance;		
		} catch (Exception e) {
			throw new BeanCreationFailedException("Class "+beanMethod.getReturnType().getName()+" failed to reate Bean.",e);
		}
	}

}
