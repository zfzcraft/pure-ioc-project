package cn.zfzcraft.pureioc.core;

public class PropertiesBeanDefinition  implements BeanDefinition{
	
	@Override
	public String toString() {
		return "PropertiesBeanDefinition [beanClass=" + beanClass + ", eager=" + eager + "]";
	}

	private Class<?> beanClass;
    
	private boolean eager;
	
	

	public PropertiesBeanDefinition(Class<?> beanClass, boolean eager) {
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


	public void setEager(boolean eager) {
		this.eager = eager;
	}

	@Override
	public boolean isEager() {
		return eager;
	}
    
	@Override
	public BeanDefinitionType beanDefinitionType() {
		return BeanDefinitionType.PROPERTIES;
	}

	
    
}
