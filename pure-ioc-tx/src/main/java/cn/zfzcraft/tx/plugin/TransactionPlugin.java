package cn.zfzcraft.tx.plugin;

import java.util.Set;

import cn.zfzcraft.pureioc.core.spi.Plugin;
import cn.zfzcraft.tx.TransactionableBeanPostProcessor;

public class TransactionPlugin implements Plugin{

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(TransactionableBeanPostProcessor.class);
	}

}
