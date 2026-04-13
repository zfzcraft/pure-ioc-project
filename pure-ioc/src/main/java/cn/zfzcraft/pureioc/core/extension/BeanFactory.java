package cn.zfzcraft.pureioc.core.extension;

import java.lang.reflect.AnnotatedElement;

import cn.zfzcraft.pureioc.annotations.QualifierClass;
import cn.zfzcraft.pureioc.core.ApplicationContext;

/**
 * must be no args constructor
 */
public interface BeanFactory{

	Object  createBean(ApplicationContext applicationContext,AnnotatedElement beanElement);
	
	default Object[] resolveArgs(ApplicationContext applicationContext, Class<?>[] types) {
		Object[] args = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			Class<?> argClass = types[i];
			if(argClass.isAnnotationPresent(QualifierClass.class)) {
				QualifierClass qualifierTargetClass = argClass.getAnnotation(QualifierClass.class);
				Class<?> beanTargetClass = qualifierTargetClass.value();
				args[i] = applicationContext.getBean(beanTargetClass);
			}else {
				args[i] = applicationContext.getBean(types[i]);
			}
		}
		return args;
	}
}
