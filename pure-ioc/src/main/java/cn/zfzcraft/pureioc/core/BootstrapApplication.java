package cn.zfzcraft.pureioc.core;

public class BootstrapApplication {

	public static ApplicationContext run(String[] args, Class<?> mainClass) {
			LifeCycleApplicationContext context = new AnnotationConfigApplicationContext();
			context.setArgs(args);
			context.setMaincClass(mainClass);
			context.refresh();
			return context;
	}
}
