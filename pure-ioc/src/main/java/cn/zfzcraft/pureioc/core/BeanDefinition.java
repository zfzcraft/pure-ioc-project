package cn.zfzcraft.pureioc.core;

public interface BeanDefinition {

	boolean isEager();
		
	BeanDefinitionType beanDefinitionType();
	
	@SuppressWarnings("unchecked")
	default <T extends BeanDefinition> T wrap() {
		return (T) this;
	}
}
