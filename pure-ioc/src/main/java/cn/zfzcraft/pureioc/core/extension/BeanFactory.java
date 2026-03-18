package cn.zfzcraft.pureioc.core.extension;

import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.ExtensionPoint;

/**
 * must be no args constructor
 */
public interface BeanFactory  extends ExtensionPoint{

	Object  createBean(ApplicationContext applicationContext,Class<?> beanClass);
}
