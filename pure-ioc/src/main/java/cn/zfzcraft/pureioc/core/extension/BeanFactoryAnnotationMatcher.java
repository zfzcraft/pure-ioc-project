package cn.zfzcraft.pureioc.core.extension;

import java.lang.annotation.Annotation;
/**
 * must be no args constructor
 */
public interface BeanFactoryAnnotationMatcher extends ExtensionPoint {

	
	/**
	 * for interface class ,must return a BeanFactoryClass
	 * 
	 * @return
	 */
	BeanFactory getBeanFactory();

	Class<? extends Annotation> getBeanAnnotationClass();
}
