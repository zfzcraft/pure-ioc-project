package pure.ioc.plugin.mybatisplus;

import org.apache.ibatis.session.SqlSessionFactory;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
@Extension
public class MapperBeanFactory implements BeanFactory{

	@Override
	public Object createBean(ApplicationContext applicationContext, Class<?> mapperClass) {
		SqlSessionFactory factory = applicationContext.getBean(SqlSessionFactory.class);
		if (!factory.getConfiguration().hasMapper(mapperClass)) {
			factory.getConfiguration().addMapper(mapperClass);
		}
		return factory.openSession().getMapper(mapperClass);
	}

}
