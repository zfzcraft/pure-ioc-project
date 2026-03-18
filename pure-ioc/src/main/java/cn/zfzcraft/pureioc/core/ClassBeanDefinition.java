package cn.zfzcraft.pureioc.core;

public class ClassBeanDefinition implements BeanDefinition {

	private Class<?> beanClass;

	private boolean eager;
	
	public ClassBeanDefinition(Class<?> beanClass, boolean eager) {
		super();
		this.beanClass = beanClass;
		this.eager = eager;
		
	}

	public Class<?> getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	@Override
	public boolean isEager() {
		return eager;
	}

	@Override
	public BeanDefinitionType beanDefinitionType() {
		return BeanDefinitionType.CLASS;
	}

	

	public void setEager(boolean eager) {
		this.eager = eager;
	}

	@Override
	public String toString() {
		return "ClassBeanDefinition [beanClass=" + beanClass + ", eager=" + eager + "]";
	}


	

}
