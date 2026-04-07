package cn.zfzcraft.pureioc.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.zfzcraft.pureioc.annotations.TargetInterface;
import cn.zfzcraft.pureioc.core.exception.LazyProxyBeanCreationFailedException;
import cn.zfzcraft.pureioc.utils.ClassInterfaceUtils;
import cn.zfzcraft.pureioc.utils.ProxyReflectUtils;

//动态代理上下文
public final class ProxyContext {

	private static ApplicationContext cachedApplicationContext;

	private static final Map<Class<?>, Object> PROXY_MAP = new ConcurrentHashMap<>();

	private ProxyContext() {
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Class<T> type) {
		if (type == null) {
			return null;
		}
		T proxy = (T) PROXY_MAP.computeIfAbsent(type, function -> {
			return createLazyProxy(type);
		});
		return proxy;
	}

	private static <T> Object createLazyProxy(Class<T> type) {
		if (type.isInterface()) {
			return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
					new LazyInvocationHandler(type));
		} else if (ClassInterfaceUtils.hasOnlyOneBusinessInterface(type)) {
			Class<?> interfaceClass = ClassInterfaceUtils.getAllBusinessInterfaces(type).iterator().next();
			return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { interfaceClass },
					new LazyInvocationHandler(type));
		} else if (type.isAnnotationPresent(TargetInterface.class)) {
			TargetInterface targetInterface = type.getAnnotation(TargetInterface.class);
			return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { targetInterface.value() },
					new LazyInvocationHandler(type));
		} else {
			throw new LazyProxyBeanCreationFailedException("Lazy Proxy Bean :" + type
					+ "CreationFailed on : must be interface or only one interface or annotated with @TargetInterface！");
		}
	}

	static class LazyInvocationHandler implements InvocationHandler {
		private Class<?> type;
		public LazyInvocationHandler(Class<?> type) {
			super();
			this.type = type;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Object beanObject = cachedApplicationContext.getBean(type);
			return ProxyReflectUtils.invokeMethod(method, beanObject, args);
		}

	}

	protected static void clear() {
		PROXY_MAP.clear();
	}

	protected static void initializeApplicationContext(AnnotationConfigApplicationContext applicationContext) {
		if (cachedApplicationContext == null) {
			cachedApplicationContext = applicationContext;
		}
	}
}