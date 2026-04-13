package cn.zfzcraft.pureioc.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.ConditionalOnClass;
import cn.zfzcraft.pureioc.annotations.ConditionalOnMissingBean;
import cn.zfzcraft.pureioc.annotations.ConditionalOnPropertity;
import cn.zfzcraft.pureioc.annotations.Configuration;
import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;
import cn.zfzcraft.pureioc.annotations.Eager;
import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.annotations.Imports;
import cn.zfzcraft.pureioc.core.exception.BeanFactoryNotFoundException;
import cn.zfzcraft.pureioc.core.exception.BeanNotExistException;
import cn.zfzcraft.pureioc.core.exception.ConstructorCircularDependencyError;
import cn.zfzcraft.pureioc.core.exception.ExtensionCreationFailedException;
import cn.zfzcraft.pureioc.core.exception.IgnoreException;
import cn.zfzcraft.pureioc.core.exception.ResourcesNotFoundException;
import cn.zfzcraft.pureioc.core.exception.TooManyBeanFactoriesException;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
import cn.zfzcraft.pureioc.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.extension.BeanPostProcessor;
import cn.zfzcraft.pureioc.core.extension.EnvironmentLoader;
import cn.zfzcraft.pureioc.core.extension.EnvironmentPostProcessor;
import cn.zfzcraft.pureioc.core.index.ClassIndexParser;
import cn.zfzcraft.pureioc.core.index.FileIndexParser;
import cn.zfzcraft.pureioc.core.spi.Plugin;
import cn.zfzcraft.pureioc.utils.ClassLoaderUtils;
import cn.zfzcraft.pureioc.utils.NestedMapUtils;
import cn.zfzcraft.pureioc.utils.ResourceUtils;

public final class AnnotationConfigApplicationContext implements LifeCycleApplicationContext {

	private static final String DOT_CLASS = ".class";

	private static final String META_INF_BEANS_INDEX = "META-INF/beans.index";

	private AtomicBoolean refresh = new AtomicBoolean(false);

	private AtomicBoolean preheated = new AtomicBoolean(false);

	private Class<?> maincClass;

	private String[] args;

	private Map<String, Object> env = new HashMap<>();

	private Set<Class<?>> pluginClasses = new HashSet<>();

	// 按 order 升序：越小越先
	private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

	private Map<Class<?>, BeanDefinition> beanDefinitionMap = new HashMap<>();

	private final Map<Class<?>, Object> singletonPool = new ConcurrentHashMap<>();

	private List<String> applicationClassNameList = new ArrayList<>();

	private Map<Class<? extends Annotation>, BeanFactory> beanFactoryMap = new ConcurrentHashMap<>();

	private Environment environment = new LocalEnvironment(env);

	private List<Class<?>> applicationClasses = new ArrayList<>();

	private Set<Class<?>> creatingBeans = Collections.newSetFromMap(new ConcurrentHashMap<>());

	@Override
	public void setMaincClass(Class<?> maincClass) {
		this.maincClass = maincClass;
	}

	@Override
	public void setArgs(String[] args) {
		this.args = args;
	}

	@Override
	public void refresh() {
		if (refresh.compareAndSet(false, true)) {

			loadPlugin();

			loadEnvironment();

			doEnvironmentLoader();

			doEnvironmentPostProcessor();

			collectBeanFactoryAnnotationMatchers();

			scanBeanClasses();

			parallelLoadClasses();

			collectBeanPostProcessors();

			registerBeanDefinitions();

			registerApplicationContext();

			initializeProxyContext();

			instantiateEagerBeans();

			System.out.println("启动容器成功............");

			asyncPreheatBeansAndClearResources();

			registerShutdownHook();
		}
	}

	private void initializeProxyContext() {
		ProxyContext.initializeApplicationContext(this);
	}

	private void instantiateEagerBeans() {
		for (Entry<Class<?>, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
			Class<?> clazz = entry.getKey();
			BeanDefinition beanDefinition = entry.getValue();
			if (beanDefinition.isEager()) {
				getBean(clazz);
			}
		}
	}

	// ==========================
	// 加载配置
	// ==========================
	private void loadEnvironment() {
		Map<String, Object> map = EnvironmentInitializer.initializeEnvironment(args);
		env.putAll(map);
	}

	private void parallelLoadClasses() {
		List<Class<?>> classes = ParallelClassLoader.load(applicationClassNameList);
		applicationClasses.addAll(classes);
	}

	private void registerBeanDefinitions() {
		for (Class<?> clazz : applicationClasses) {
			registerBeanDefinition(clazz);
		}
		for (Class<?> clazz : pluginClasses) {
			registerBeanDefinition(clazz);
		}
	}

	private void asyncPreheatBeansAndClearResources() {
		String prefix = EnvironmentProperties.class.getAnnotation(ConfigurationProperties.class).prefix();
		EnvironmentProperties environmentProperties = NestedMapUtils.loadAs(env, prefix, EnvironmentProperties.class);
		if (environmentProperties.isPreheat()) {
			new Thread(() -> {
				preheatLazyBeans();
				preheated.compareAndSet(false, true);
				clearResources();
			}).start();
		}
	}

	private void preheatLazyBeans() {
		for (Entry<Class<?>, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
			Class<?> clazz = entry.getKey();
			BeanDefinition beanDefinition = entry.getValue();
			if (!beanDefinition.isEager()) {
				getBean(clazz);
			}
		}
	}

	private void clearResources() {
		pluginClasses.clear();
		beanPostProcessors.clear();
		beanDefinitionMap.clear();
		applicationClassNameList.clear();
		beanFactoryMap.clear();
		applicationClasses.clear();
		creatingBeans.clear();
		pluginClasses = null;
		beanPostProcessors = null;
		beanDefinitionMap = null;
		applicationClassNameList = null;
		beanFactoryMap = null;
		applicationClasses = null;
		creatingBeans = null;
	}

	private void registerApplicationContext() {
		singletonPool.putIfAbsent(ApplicationContext.class, this);
	}

	private void doEnvironmentPostProcessor() {
		for (Class<?> clazz : pluginClasses) {
			if (EnvironmentPostProcessor.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(Extension.class)) {
				try {
					EnvironmentPostProcessor environmentPostProcessor = (EnvironmentPostProcessor) clazz
							.getConstructor().newInstance();
					environmentPostProcessor.process(environment);
				} catch (Exception e) {
					throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
				}
			}
		}
	}

	private void doEnvironmentLoader() {
		List<Class<?>> list = pluginClasses.stream().filter(
				ele -> EnvironmentLoader.class.isAssignableFrom(ele) && ele.isAnnotationPresent(Extension.class))
				.collect(Collectors.toList());
		for (Class<?> clazz : list) {
			try {
				EnvironmentLoader loader = (EnvironmentLoader) clazz.getConstructor().newInstance();
				Map<String, Object> networkMap = loader.load(environment);
				EnvironmentInitializer.deepMerge(env, networkMap);
			} catch (Exception e) {
				throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
			}
		}
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			destory();
		}));
	}

	private void collectBeanFactoryAnnotationMatchers() {
		for (Class<?> clazz : pluginClasses) {
			if (BeanFactoryAnnotationMatcher.class.isAssignableFrom(clazz)
					&& clazz.isAnnotationPresent(Extension.class)) {
				try {
					Constructor<?> constructor = clazz.getConstructor();
					BeanFactoryAnnotationMatcher beanFactoryAnnotationMatcher = (BeanFactoryAnnotationMatcher) constructor
							.newInstance();
					beanFactoryMap.putIfAbsent(beanFactoryAnnotationMatcher.getBeanAnnotationClass(),
							beanFactoryAnnotationMatcher.getBeanFactory());
				} catch (Exception e) {
					throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
				}
			}
		}
	}

	private void collectBeanPostProcessors() {
		for (Class<?> clazz : pluginClasses) {
			if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
				try {
					Constructor<?> constructor = clazz.getConstructor();
					BeanPostProcessor beanPostProcessor = (BeanPostProcessor) constructor.newInstance();
					beanPostProcessors.add(beanPostProcessor);
				} catch (Exception e) {
					throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
				}
			}
		}
		beanPostProcessors.sort(Comparator.comparingInt(BeanPostProcessor::getOrder));
	}

	private void scanBeanClasses() {
		if (ResourceUtils.exists(META_INF_BEANS_INDEX)) {
			scanFileIndex();
		} else {
			scanPackage();
		}
	}

	private void scanFileIndex() {
		URL url = ResourceUtils.getResource(META_INF_BEANS_INDEX);
		if (url == null) {
			throw new ResourcesNotFoundException("resource " + META_INF_BEANS_INDEX + " not found!");
		}
		try (InputStream is = url.openStream()) {
			List<String> classList = FileIndexParser.parse(is, beanFactoryMap.keySet());
			applicationClassNameList.addAll(classList);
		} catch (IOException e) {
			throw new IgnoreException("ignore", e);
		}
	}

	private void scanPackage() {
		List<String> classList = ClassIndexParser.scanByPackage(maincClass.getPackageName(), beanFactoryMap.keySet());
		applicationClassNameList.addAll(classList);
	}

	private boolean isAnnotationClass(Class<?> clazz) {
		for (Class<? extends Annotation> annotationClass : beanFactoryMap.keySet()) {
			if (clazz.isAnnotationPresent(annotationClass)) {
				return true;
			}
		}
		return false;
	}

	private void loadPlugin() {
		Set<Class<?>> tempPluginClasses = new HashSet<>();
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader();
		ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, classLoader);
		for (Plugin plugin : loader) {
			plugin.registerBeanClasses(tempPluginClasses);
		}
		for (Class<?> loadClass : tempPluginClasses) {
			pluginClasses.add(loadClass);
			if (loadClass.isAnnotationPresent(Imports.class) && loadClass.isAnnotationPresent(Configuration.class)) {
				Imports imports = loadClass.getAnnotation(Imports.class);
				for (Class<?> clazz : imports.value()) {
					pluginClasses.add(clazz);
				}
			}
		}
	}

	private void registerBeanDefinition(Class<?> clazz) {
		if (isAnnotationClass(clazz)) {
			boolean eager = getEager(clazz);
			BeanFactory beanFactory = getBeanFactory(clazz.getAnnotations());
			beanDefinitionMap.putIfAbsent(clazz, new BeanDefinition(clazz, eager, beanFactory));
			if (clazz.isAnnotationPresent(Configuration.class)) {
				registerMethodBeanDefinition(clazz);
			}
		}
	}

	private void registerMethodBeanDefinition(Class<?> clazz) {
		for (Method beanMethod : clazz.getDeclaredMethods()) {
			if (hasCondition(beanMethod)) {
				if (isConditionTrue(beanMethod)) {
					registerMethodBeanMetaInfo(beanMethod);
				}
			} else {
				registerMethodBeanMetaInfo(beanMethod);
			}
		}
	}

	private void registerMethodBeanMetaInfo(Method beanMethod) {
		if (beanMethod.isAnnotationPresent(Bean.class)) {
			boolean eagerMethod = getEager(beanMethod);
			Class<?> returnType = beanMethod.getReturnType();
			BeanFactory beanFactory = getBeanFactory(beanMethod.getAnnotations());
			beanDefinitionMap.putIfAbsent(returnType, new BeanDefinition(beanMethod, eagerMethod, beanFactory));
		}
	}

	private boolean getEager(Method beanMethod) {
		if (beanMethod.isAnnotationPresent(Eager.class)) {
			return true;
		}
		return false;
	}

	private boolean getEager(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Eager.class)) {
			return true;
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clazz) {
		Object bean = singletonPool.get(clazz);
		if (bean != null) {
			return (T) bean;
		}
		synchronized (clazz) {
			bean = singletonPool.get(clazz);
			if (bean == null) {
				if (creatingBeans.contains(clazz)) {
					throw new ConstructorCircularDependencyError("Constructor Circular Dependency Error on Class :" + clazz);
				}
				creatingBeans.add(clazz);
				bean = createBean(clazz);
				singletonPool.put(clazz, bean);
			}
		}
		return (T) bean;
	}

	private Object createBean(Class<?> clazz) {
		BeanDefinition beanDefinition = beanDefinitionMap.get(clazz);
		if (beanDefinition == null) {
			throw new BeanNotExistException("Bean " + clazz.getName() + " Not Exist ");
		}
		Object beanObject = beanDefinition.getBeanFactory().createBean(this, beanDefinition.getBeanElement());
		if (beanObject == null) {
			throw new BeanNotExistException("Bean " + clazz + " Not Exist!");
		}
		for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
			if (beanPostProcessor.matche(clazz)) {
				beanObject = beanPostProcessor.process(this, clazz, beanObject);
			}
		}
		return beanObject;
	}

	private BeanFactory getBeanFactory(Annotation[] annotations) {
		List<BeanFactory> beanFactories = new ArrayList<>();
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> annotationClass = annotation.annotationType();
			BeanFactory beanFactory = beanFactoryMap.get(annotationClass);
			if (beanFactory != null) {
				beanFactories.add(beanFactory);
			}
		}
		if (beanFactories.isEmpty()) {
			throw new BeanFactoryNotFoundException("Not Found Bean Factory!");
		}
		if (beanFactories.size() > 1) {
			throw new TooManyBeanFactoriesException("Too Many Bean Factories!");
		}
		return beanFactories.get(0);
	}

	@Override
	public void destory() {
		for (Entry<Class<?>, Object> entry : singletonPool.entrySet()) {
			Object bean = entry.getValue();
			if (bean instanceof DisposableBean disposableBean) {
				disposableBean.destroy();
			}
		}
		singletonPool.clear();
	}

	/**
	 * 判断类是否存在（标准 JVM 方式：仅检查 .class 资源，不加载类）
	 * 
	 * @param className 全类名，如 com.zaxxer.hikari.HikariDataSource
	 * @return 存在返回 true，不存在 false
	 */
	private boolean isClassPresent(String className) {
		if (className == null || className.isBlank()) {
			return false;
		}
		String resourceName = className.replace('.', '/') + DOT_CLASS;
		return ResourceUtils.getResource(resourceName) != null;
	}

	private boolean matchesProperty(Map<String, Object> config, String propertyKey, Object havingValue) {
		if (config == null || config.isEmpty()) {
			return false;
		}
		Object actualValue = NestedMapUtils.getNestedValue(config, propertyKey);
		if (actualValue == null) {
			return false;
		}
		return isValueMatch(actualValue, havingValue);
	}

	private boolean isValueMatch(Object actual, Object expected) {
		if (Objects.equals(actual, expected)) {
			return true;
		}
		// 布尔宽松匹配：true/"true"/"TRUE"/"True" 都算 true
		if (expected instanceof Boolean) {
			String actualStr = actual.toString().trim().toLowerCase();
			return Boolean.parseBoolean(actualStr) == (Boolean) expected;
		}
		// 字符串忽略大小写匹配
		if (expected instanceof String && actual instanceof String) {
			return ((String) expected).equalsIgnoreCase((String) actual);
		}
		return false;
	}

	private boolean hasCondition(Method method) {
		if (method.isAnnotationPresent(ConditionalOnClass.class)) {
			return true;
		}
		if (method.isAnnotationPresent(ConditionalOnMissingBean.class)) {
			return true;
		}
		if (method.isAnnotationPresent(ConditionalOnPropertity.class)) {
			return true;
		}
		return false;
	}

	private boolean isConditionTrue(Method method) {
		if (method.isAnnotationPresent(ConditionalOnClass.class)) {
			ConditionalOnClass conditionalOnClass = method.getAnnotation(ConditionalOnClass.class);
			String className = conditionalOnClass.className();
			if (!isClassPresent(className)) {
				return false;
			}
		}
		if (method.isAnnotationPresent(ConditionalOnPropertity.class)) {
			ConditionalOnPropertity conditionalOnPropertity = method.getAnnotation(ConditionalOnPropertity.class);
			String key = conditionalOnPropertity.key();
			String value = conditionalOnPropertity.value();
			if (!matchesProperty(env, key, value)) {
				return false;
			}
		}
		if (method.isAnnotationPresent(ConditionalOnMissingBean.class)) {
			Class<?> key = method.getReturnType();
			if (key.isInterface()) {
				List<Class<?>> classNames = getImplementationClasses(key);
				if (classNames != null && !classNames.isEmpty()) {
					return false;
				}
			} else {
				boolean exist = applicationClasses.contains(key);
				if (exist) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Environment getEnvironment() {
		return environment;
	}

	@Override
	public List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
		List<Class<?>> classes = new ArrayList<>();
		if (preheated.get() == false) {
			for (Class<?> clazz : applicationClasses) {
				if (clazz.isAnnotationPresent(annotationClass)) {
					classes.add(clazz);
				}
			}
			for (Class<?> clazz : pluginClasses) {
				if (clazz.isAnnotationPresent(annotationClass)) {
					classes.add(clazz);
				}
			}
		} else {
			for (Class<?> clazz : singletonPool.keySet()) {
				if (clazz.isAnnotationPresent(annotationClass)) {
					classes.add(clazz);
				}
			}
		}
		return classes;
	}

	@Override
	public List<Class<?>> getImplementationClasses(Class<?> interfaceClass) {
		List<Class<?>> classes = new ArrayList<>();
		if (preheated.get() == false) {
			for (Class<?> clazz : applicationClasses) {
				if (interfaceClass.isAssignableFrom(clazz)) {
					classes.add(clazz);
				}
			}
			for (Class<?> clazz : pluginClasses) {
				if (interfaceClass.isAssignableFrom(clazz)) {
					classes.add(clazz);
				}
			}
		} else {
			for (Class<?> clazz : singletonPool.keySet()) {
				if (interfaceClass.isAssignableFrom(clazz)) {
					classes.add(clazz);
				}
			}
		}
		return classes;
	}

}