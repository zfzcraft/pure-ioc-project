package cn.zfzcraft.tx;

import java.lang.reflect.Proxy;

import javax.sql.DataSource;

import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.extension.BeanPostProcessor;
@Extension
public class TransactionableBeanPostProcessor implements BeanPostProcessor{

	@Override
	public boolean matche(Class<?> beanClass) {
		return beanClass.isAnnotationPresent(Transactionable.class);
	}

	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Object process(ApplicationContext applicationContext, Class<?> beanName, Object bean) {
		DataSource dataSource = applicationContext.getBean(DataSource.class);
		Transactionable transactionable =	bean.getClass().getAnnotation(Transactionable.class);
		Class<?> proxyInterfaceClass = transactionable.proxyInterfaceClass();
		return Proxy.newProxyInstance(proxyInterfaceClass.getClassLoader(), new Class[] {proxyInterfaceClass}, new TransactionableInvocationHandler(dataSource));
	}

}
