package cn.zfzcraft.pureioc.core.extension;

import java.lang.annotation.Annotation;

import cn.zfzcraft.pureioc.core.ExtensionPoint;

/**
 * must be no args constructor
 */
public interface BeanFactoryAnnotationMatcher extends ExtensionPoint {

	Class<? extends BeanFactory> DEFAULT_NULL_FACTORY = null;

	/**
	 * for interface class ,must return a BeanFactoryClass
	 * 
	 * @return
	 */
	default Class<? extends BeanFactory> getBeanFactoryClass() {
		return DEFAULT_NULL_FACTORY;
	}

	Class<? extends Annotation> getBeanAnnotationClass();
}
