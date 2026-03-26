package cn.zfzcraft.pureioc.core.matcher;

import java.lang.annotation.Annotation;

import cn.zfzcraft.pureioc.annotations.Compoment;
import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.factory.CompomentBeanFactory;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
@Extension
public class CompomentBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	@Override
	public BeanFactory getBeanFactory() {
		return new CompomentBeanFactory();
	}

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Compoment.class;
	}

}
