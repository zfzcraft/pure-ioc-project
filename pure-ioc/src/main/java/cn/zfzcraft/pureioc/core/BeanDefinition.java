package cn.zfzcraft.pureioc.core;

import java.lang.reflect.AnnotatedElement;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;

public class BeanDefinition {
	
	
	
	private AnnotatedElement beanElement;
	
	private boolean eager;
	
	private BeanFactory beanFactory;
	
	public boolean isEager() {
		return eager;
	}

	public void setEager(boolean eager) {
		this.eager = eager;
	}

	public AnnotatedElement getBeanElement() {
		return beanElement;
	}

	public void setBeanElement(AnnotatedElement beanElement) {
		this.beanElement = beanElement;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	

	public BeanDefinition(AnnotatedElement beanElement, boolean eager, BeanFactory beanFactory) {
		super();
		this.beanElement = beanElement;
		this.eager = eager;
		this.beanFactory = beanFactory;
	}

}
