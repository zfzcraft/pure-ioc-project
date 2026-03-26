package cn.zfzcraft.tx;

import javax.sql.DataSource;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.extension.BeanPostProcessor;
@Extension
public class TransactionDataSourceBeanPostProcessor implements BeanPostProcessor{

	@Override
	public boolean matche(Class<?> beanClass) {
		return DataSource.class.isAssignableFrom(beanClass);
	}

	

	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}



	@Override
	public Object process(ApplicationContext applicationContext, Class<?> beanName, Object bean) {
		DataSource dataSource = (DataSource) bean;
		TransactionDataSource transactionDataSource = new TransactionDataSource(dataSource);
		return transactionDataSource;
	}

}
