package cn.zfzcraft.pureioc.core.extension;

import cn.zfzcraft.pureioc.core.ApplicationContext;
/**
 * must be no args constructor
 */
public interface BeanPostProcessor extends ExtensionPoint {
	
	
	boolean matche(Class<?> beanClass);
	

	Object process(ApplicationContext applicationContext,Class<?> beanName, Object bean);

	/**
	 * Smaller order executes earlier; larger order executes later.
	 * 
	 * @return
	 */
	int getOrder();

}
