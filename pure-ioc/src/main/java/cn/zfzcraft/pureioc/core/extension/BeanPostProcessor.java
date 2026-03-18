package cn.zfzcraft.pureioc.core.extension;

import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.ExtensionPoint;
/**
 * must be no args constructor
 */
public interface BeanPostProcessor extends ExtensionPoint {
	
	
	boolean matche(Class<?> beanClass);
	

	Object postProcess(ApplicationContext applicationContext, Object bean);

	/**
	 * Smaller order executes earlier; larger order executes later.
	 * 
	 * @return
	 */
	int getOrder();

}
