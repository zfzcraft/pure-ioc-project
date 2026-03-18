package cn.zfzcraft.pureioc.core;

import java.lang.reflect.Method;

public class MethodBeanDefinition implements BeanDefinition {

	@Override
	public String toString() {
		return "MethodBeanDefinition [configurationClass=" + configurationClass + ", beanMethod=" + beanMethod
				+ ", eager=" + eager + "]";
	}

	private Class<?> configurationClass;

	private Method beanMethod;

	private boolean eager;

	public MethodBeanDefinition(Class<?> configurationClass, Method beanMethod, boolean eager) {
		super();
		this.configurationClass = configurationClass;
		this.beanMethod = beanMethod;
		this.eager = eager;
	}

	public Class<?> getConfigurationClass() {
		return configurationClass;
	}

	public void setConfigurationClass(Class<?> configurationClass) {
		this.configurationClass = configurationClass;
	}

	public Method getBeanMethod() {
		return beanMethod;
	}

	public void setBeanMethod(Method beanMethod) {
		this.beanMethod = beanMethod;
	}

	@Override
	public boolean isEager() {
		return eager;
	}

	@Override
	public BeanDefinitionType beanDefinitionType() {
		return BeanDefinitionType.METHOD;
	}

}
