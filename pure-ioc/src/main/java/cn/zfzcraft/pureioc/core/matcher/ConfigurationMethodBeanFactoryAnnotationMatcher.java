package cn.zfzcraft.pureioc.core.matcher;

import java.lang.annotation.Annotation;

import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
import cn.zfzcraft.pureioc.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.factory.ConfigurationMethodBeanFactory;

@Extension
public class ConfigurationMethodBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	@Override
	public BeanFactory getBeanFactory() {
		return new ConfigurationMethodBeanFactory();
	}

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Bean.class;
	}

}
