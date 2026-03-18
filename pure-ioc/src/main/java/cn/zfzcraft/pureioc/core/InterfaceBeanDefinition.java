package cn.zfzcraft.pureioc.core;

import cn.zfzcraft.pureioc.core.extension.BeanFactory;

public class InterfaceBeanDefinition implements BeanDefinition {

	@Override
	public String toString() {
		return "InterfaceBeanDefinition [beanClass=" + beanClass + ", beanFactory=" + beanFactory + ", eager=" + eager
				+ "]";
	}
	private Class<?> beanClass;
	

	private Class<? extends BeanFactory> beanFactory;
	private boolean eager;
	
	
	
	
	public InterfaceBeanDefinition(Class<?> beanClass, Class<? extends BeanFactory> beanFactory, boolean eager) {
		super();
		this.beanClass = beanClass;
		this.beanFactory = beanFactory;
		this.eager = eager;
	}


	public Class<?> getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class<?> beanClass) {
		this.beanClass = beanClass;
	}


	public Class<? extends BeanFactory> getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(Class<? extends BeanFactory> beanFactory) {
		this.beanFactory = beanFactory;
	}

	

	@Override
	public boolean isEager() {
		return eager;
	}
	@Override
	public BeanDefinitionType beanDefinitionType() {
		return BeanDefinitionType.INTERFACE;
	}
}
