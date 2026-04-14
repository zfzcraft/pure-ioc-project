package cn.zfzcraft.pureioc.core.factory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;

import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.exception.BeanCreationFailedException;
import cn.zfzcraft.pureioc.core.exception.TooManyConstructorsException;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;


public class CompomentBeanFactory implements BeanFactory{

	@Override
	public Object createBean(ApplicationContext applicationContext, AnnotatedElement beanElement) {
		Class<?> beanClass = (Class<?>) beanElement;
		Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
		if (constructors.length>1) {
			throw new TooManyConstructorsException("Class "+beanClass.getName()+" has too many Declared Constructors. Only one Declared Constructor is allowed.");
		}
		Constructor<?> ctor = constructors[0];
		Object[] args = resolveArgs(applicationContext,ctor.getParameterTypes());
		try {
			Object instance = ctor.newInstance(args);
			return instance;
		} catch (Exception e) {
			throw new BeanCreationFailedException("Class "+beanClass.getName()+" failed to create Bean.",e);
		}
	}
	
}
