package pure.ioc.plugin.mybatisplus;

import java.lang.reflect.AnnotatedElement;

import org.apache.ibatis.session.SqlSessionFactory;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
@Extension
public class MapperBeanFactory implements BeanFactory{

	
	@Override
	public Object createBean(ApplicationContext applicationContext, AnnotatedElement beanElement) {
		Class<?> mapperClass  =(Class<?>) beanElement;
		SqlSessionFactory factory = applicationContext.getBean(SqlSessionFactory.class);
		if (!factory.getConfiguration().hasMapper(mapperClass)) {
			factory.getConfiguration().addMapper(mapperClass);
		}
		return factory.openSession().getMapper(mapperClass);
	}

}
