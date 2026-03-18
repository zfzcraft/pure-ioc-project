package cn.zfzcraft.pureioc.core;

public interface LifeCycleApplicationContext extends ApplicationContext {
	
	

	void refresh();
	
	void destory();

	void setArgs(String[] args);

	void setMaincClass(Class<?> maincClass);
}
