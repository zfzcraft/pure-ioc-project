package pure.ioc.plugin.mybatisplus;

import java.lang.annotation.Annotation;

import org.apache.ibatis.annotations.Mapper;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
import cn.zfzcraft.pureioc.core.extension.BeanFactoryAnnotationMatcher;

@Extension
public class MybatisPlusBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher {

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Mapper.class;
	}

	@Override
	public BeanFactory getBeanFactory() {
		return new MapperBeanFactory();
	}
}
