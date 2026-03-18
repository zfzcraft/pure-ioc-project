package cn.zfzcraft.pureioc.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.yaml.snakeyaml.Yaml;
import cn.zfzcraft.pureioc.annotations.Bean;
import cn.zfzcraft.pureioc.annotations.Component;
import cn.zfzcraft.pureioc.annotations.ConditionalOnClass;
import cn.zfzcraft.pureioc.annotations.ConditionalOnMissingBean;
import cn.zfzcraft.pureioc.annotations.ConditionalOnPropertity;
import cn.zfzcraft.pureioc.annotations.Configuration;
import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;
import cn.zfzcraft.pureioc.annotations.Eager;
import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.annotations.Imports;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;
import cn.zfzcraft.pureioc.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.pureioc.core.extension.BeanPostProcessor;
import cn.zfzcraft.pureioc.core.extension.EnvironmentLoader;
import cn.zfzcraft.pureioc.core.extension.EnvironmentPostProcessor;
import cn.zfzcraft.pureioc.core.spi.Plugin;
import cn.zfzcraft.pureioc.utils.AnnotationUtils;
import cn.zfzcraft.pureioc.utils.NestedMapUtils;

public final class AnnotationConfigApplicationContext implements LifeCycleApplicationContext {

	private static final String PACKAGE_INFO = "package-info";

	private static final String PACKAGE_INFO_CLASS = "package-info.class";

	private static final String DOT_CLASS = ".class";

	private static final String JAR = "jar";

	private static final String FILE = "file";

	private static final String META_INF_BEANS_INDEX = "META-INF/bean_classes.index";

	private static final String ENV = "env";

	private static final char POINT = '.';

	private static final String EMPTY = "";

	private static final String LINE = "-";

	private static final String YML = ".yml";

	private static final String APP = "app";

	private static final String BASE_CONFIG_FILE = "app.yml";

	private static final Yaml YAML = new Yaml();

	private AtomicBoolean refresh = new AtomicBoolean(false);

	private AtomicBoolean preheatComplete = new AtomicBoolean(false);

	private Class<?> maincClass;

	private String[] args;

	private Map<String, Object> env = new HashMap<>();

	private List<Plugin> plugins = new ArrayList<>();

	private List<Class<?>> beanPostProcessorClasses = new ArrayList<>();

	private List<Class<?>> applicationClasses = new ArrayList<>();

	private Set<Class<?>> tempPluginClasses = new HashSet<>();

	private Set<Class<?>> pluginClasses = new HashSet<>();

	private List<Class<? extends Annotation>> beanAnnotationClasses = new ArrayList<>();

	private List<Class<?>> beanFactoryClasses = new ArrayList<>();

	private List<BeanFactoryAnnotationMatcher> beanFactoryAnnotationMatchers = new ArrayList<>();

	// 按 order 升序：越小越先
	private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

	private Map<Class<?>, BeanDefinition> beanDefinitionMap = new HashMap<>();

	private Set<Class<?>> beanClassSet = new HashSet<>();

	private final Map<Class<?>, Object> singletonPool = new ConcurrentHashMap<>();

	private Map<Class<?>, Object> configurationMap = new ConcurrentHashMap<>();

	private Map<Class<? extends BeanFactory>, BeanFactory> beanFactoryMap = new ConcurrentHashMap<>();

	// CPU核心数
	private static final int CPU = Runtime.getRuntime().availableProcessors();
	// 线程数量 = CPU * 2
	private static final int THREAD = CPU * 2;

	// 自定义线程池
	private static ThreadPoolExecutor ThreadPool = new ThreadPoolExecutor(THREAD, THREAD, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(256), new ThreadPoolExecutor.CallerRunsPolicy());

	private Environment environment = new LocalEnvironment(env);

	{
		beanAnnotationClasses.add(Bean.class);
		beanAnnotationClasses.add(Component.class);
		beanAnnotationClasses.add(ConfigurationProperties.class);
		beanAnnotationClasses.add(Configuration.class);
		beanAnnotationClasses.add(Extension.class);
	}

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

			loadEnvironment();

			loadPlugin();

			loadPluginClasses();

			loadNetworkEnvironment();

			collectFactoryBeanMatchers();

			collectBeanFactoryAndAnnotations();

			scanPackageClasses();

			postProcessEnvironment();

			collectBeanPostProcessors();

			registerBeanDefinitions();

			registerFrameworkCompoment();

			instantiateBeanPostProcessors();

			instantiateEagerBeans();

			System.out.println("启动容器成功............");

			asyncInstantiateLazyBeansAndClearResources();

			registerShutdownHook();
		}
	}

	private void postProcessEnvironment() {
		for (Class<?> clazz : pluginClasses) {
			if (EnvironmentPostProcessor.class.isAssignableFrom(clazz)) {
				try {
					EnvironmentPostProcessor environmentPostProcessor = (EnvironmentPostProcessor) clazz
							.getConstructor().newInstance();
					environmentPostProcessor.postProcess(environment);
				} catch (Exception e) {
					throw IocException.of(e);
				}
			}
		}

		for (Class<?> clazz : applicationClasses) {
			if (EnvironmentPostProcessor.class.isAssignableFrom(clazz)) {
				try {
					EnvironmentPostProcessor environmentPostProcessor = (EnvironmentPostProcessor) clazz
							.getConstructor().newInstance();
					environmentPostProcessor.postProcess(environment);
				} catch (Exception e) {
					throw IocException.of(e);
				}
			}
		}
	}

	private void loadNetworkEnvironment() {
		List<Class<?>> list = pluginClasses.stream().filter(ele -> EnvironmentLoader.class.isAssignableFrom(ele))
				.collect(Collectors.toList());
		for (Class<?> networkEnvironmentLoaderClass : list) {
			try {
				EnvironmentLoader loader = (EnvironmentLoader) networkEnvironmentLoaderClass.getConstructor()
						.newInstance();
				Map<String, Object> networkMap = loader.load(environment);
				deepMerge(env, networkMap);
			} catch (Exception e) {
				throw IocException.of(e);
			}
		}
	}

	/**
	 * 深度合并两个嵌套Map： 1. Map类型递归合并 2. List类型追加合并 3. 基础类型直接覆盖
	 */
	@SuppressWarnings("unchecked")
	private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			String key = entry.getKey();
			Object sourceValue = entry.getValue();
			Object targetValue = target.get(key);
			// 跳过null值（避免覆盖已有有效值）
			if (sourceValue == null) {
				continue;
			}
			// 场景1：目标和源都是Map → 递归合并
			if (targetValue instanceof Map && sourceValue instanceof Map) {
				deepMerge((Map<String, Object>) targetValue, (Map<String, Object>) sourceValue);
			}
			// 场景2：目标和源都是List → 追加合并
			else if (targetValue instanceof List && sourceValue instanceof List) {
				((List<Object>) targetValue).addAll((List<Object>) sourceValue);
			}
			// 场景3：基础类型/其他类型 → 直接覆盖
			else {
				target.put(key, sourceValue);
			}
		}
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			destory();
		}));
	}

	private void cleanResource() {
		maincClass = null;
		args = null;
		env.clear();
		env = null;
		configurationMap.clear();
		configurationMap = null;
		beanFactoryMap.clear();
		plugins.clear();
		beanPostProcessorClasses.clear();
		pluginClasses.clear();
		beanAnnotationClasses.clear();
		beanFactoryClasses.clear();
		beanFactoryAnnotationMatchers.clear();
		beanDefinitionMap.clear();
		beanPostProcessors.clear();
		applicationClasses.clear();
		beanFactoryMap = null;
		plugins = null;
		beanPostProcessorClasses = null;
		pluginClasses = null;
		beanAnnotationClasses = null;
		beanFactoryClasses = null;
		beanFactoryAnnotationMatchers = null;
		beanDefinitionMap = null;
		beanPostProcessors = null;
		applicationClasses = null;
	}

	private void registerFrameworkCompoment() {
		beanDefinitionMap.putIfAbsent(ApplicationContext.class, new ClassBeanDefinition(this.getClass(), false));
		singletonPool.putIfAbsent(ApplicationContext.class, this);
		beanClassSet.add(ApplicationContext.class);
		beanDefinitionMap.putIfAbsent(Environment.class, new ClassBeanDefinition(LocalEnvironment.class, false));
		singletonPool.putIfAbsent(Environment.class, environment);
		beanClassSet.add(Environment.class);
	}

	private void asyncInstantiateLazyBeansAndClearResources() {
		 Thread thread = new Thread(() -> {
		for (Entry<Class<?>, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
			Class<?> key = entry.getKey();
			BeanDefinition value = entry.getValue();
			if (!value.isEager()) {
				getBean(key);
			}
		}
		System.out.println("异步预热成功............");
		preheatComplete.compareAndSet(false, true);
		cleanResource();
		System.out.println("清理资源成功............");
		 });
		 thread.start();
	}

	private void collectFactoryBeanMatchers() {
		for (Class<?> clazz : pluginClasses) {
			if (BeanFactoryAnnotationMatcher.class.isAssignableFrom(clazz)) {
				try {
					beanFactoryAnnotationMatchers
							.add((BeanFactoryAnnotationMatcher) clazz.getConstructor().newInstance());
				} catch (Exception e) {
					throw IocException.of(e);
				}
			}
		}
	}

	private void collectBeanPostProcessors() {
		for (Class<?> clazz : applicationClasses) {
			if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
				beanPostProcessorClasses.add(clazz);
			}
		}
		for (Class<?> clazz : pluginClasses) {
			if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
				beanPostProcessorClasses.add(clazz);
			}
		}
	}

	private void collectBeanFactoryAndAnnotations() {
		for (BeanFactoryAnnotationMatcher factoryBeanMatcher : beanFactoryAnnotationMatchers) {
			beanAnnotationClasses.add(factoryBeanMatcher.getBeanAnnotationClass());
			beanFactoryClasses.add(factoryBeanMatcher.getBeanAnnotationClass());
		}
	}

	private void loadPluginClasses() {
		for (Plugin plugin : plugins) {
			plugin.registerBeanClasses(tempPluginClasses);
		}
		for (Class<?> loadClass : tempPluginClasses) {
			if (isAnnotationClass(loadClass)) {
				pluginClasses.add(loadClass);
				if (loadClass.isAnnotationPresent(Imports.class)
						&& loadClass.isAnnotationPresent(Configuration.class)) {
					Imports imports = loadClass.getAnnotation(Imports.class);
					for (Class<?> clazz : imports.value()) {
						if (isAnnotationClass(clazz)) {
							pluginClasses.add(clazz);
						}

					}
				}
			}
		}

	}

	// ==========================
	// 加载配置
	// ==========================
	private void loadEnvironment() {
		// 1. 加载基础配置 app.yml
		Map<String, Object> ymlConfig = loadYamlResource(BASE_CONFIG_FILE);
		env.putAll(ymlConfig);
		// 启动参数扁平化map
		Map<String, String> argsMap = parseMainArguments(args);
		// 转嵌套map
		Map<String, Object> nestedArgsMap = flatMapToNestedMap(argsMap);
		// 启动参数覆盖所有配置
		deepMerge(env, nestedArgsMap);
		// 确定激活的环境（命令行优先，其次是配置文件）
		String activeProfile = determineActiveProfile();
		// 加载并合并环境配置 application-{active}.yml
		if (isNotEmpty(activeProfile)) {
			String envConfigFile = APP + LINE + activeProfile + YML;
			Map<String, Object> envConfig = loadYamlResource(envConfigFile);
			// 活动参数覆盖所有配置
			deepMerge(env, envConfig);
			// 启动参数覆盖所有配置（最高优先级）
			deepMerge(env, nestedArgsMap);
		}
	}

	private boolean isNotEmpty(String activeProfile) {
		return activeProfile != null && activeProfile != EMPTY;
	}

	/**
	 * 把 k=v 格式的启动参数转换成 嵌套 Map<String, Object> 支持： key=value a.b.c=123 arr[0]=aaa
	 * arr[1]=bbb
	 */
	private Map<String, Object> flatMapToNestedMap(Map<String, String> args) {
		Map<String, Object> root = new HashMap<>();
		for (Map.Entry<String, String> entry : args.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			put(root, key, value);
		}
		return root;
	}

	@SuppressWarnings("unchecked")
	private void put(Map<String, Object> root, String key, String value) {
		String[] parts = key.split("\\.");
		Map<String, Object> current = root;
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			boolean isLast = i == parts.length - 1;
			// 处理数组：arr[0]
			if (part.contains("[")) {
				String arrayName = part.substring(0, part.indexOf("["));
				int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
				List<Object> array = getOrCreate(current, arrayName, List.class);
				while (array.size() <= index) {
					array.add(null);
				}
				if (isLast) {
					array.set(index, value);
				} else {
					Map<String, Object> node = getOrCreate(array, index, Map.class);
					current = node;
				}
				return;
			}
			// 普通层级 a.b.c
			if (isLast) {
				current.put(part, value);
			} else {
				current = getOrCreate(current, part, Map.class);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getOrCreate(Map<String, Object> map, String key, Class<T> type) {
		Object obj = map.get(key);
		if (obj == null) {
			obj = type == Map.class ? new HashMap<String, Object>() : new ArrayList<>();
			map.put(key, obj);
		}
		return (T) obj;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> getOrCreate(List<Object> list, int index, Class<Map> type) {
		while (list.size() <= index) {
			list.add(null);
		}
		Object obj = list.get(index);
		if (obj == null) {
			obj = new HashMap<String, Object>();
			list.set(index, obj);
		}
		return (Map<String, Object>) obj;
	}

	private String determineActiveProfile() {
		EnvironmentProperties environmentProperties = NestedMapUtils.loadAs(env, ENV, EnvironmentProperties.class);
		String active = environmentProperties.getActive();
		return active == null ? null : active.trim();
	}

	/**
	 * 从类路径加载YAML文件，返回嵌套Map（空安全）
	 */
	private Map<String, Object> loadYamlResource(String fileName) {
		// 空文件直接返回空Map
		if (fileName == null || fileName.isBlank()) {
			return new LinkedHashMap<>();
		}
		try (InputStream inputStream = ResourceLoader.load(fileName)) {
			// 文件不存在返回空Map
			if (inputStream == null) {
				return new LinkedHashMap<>();
			}
			// 解析YAML（SnakeYAML返回null表示空文件）
			Map<String, Object> yamlMap = YAML.load(inputStream);
			return yamlMap == null ? new LinkedHashMap<>() : yamlMap;
		} catch (Exception e) {
			throw IocException.of(e);
		}
	}

	/**
	 * 解析main启动参数：--key=value → 扁平Map
	 */
	private Map<String, String> parseMainArguments(String[] args) {
		Map<String, String> argsMap = new HashMap<>();
		// 空参数直接返回
		if (args == null || args.length == 0) {
			return argsMap;
		}
		for (String arg : args) {
			// 只处理--开头的参数
			if (arg != null && arg.startsWith("--")) {
				String[] kv = arg.substring(2).split("=", 2);
				// 确保是合法的key=value格式
				if (kv.length == 2 && kv[0] != null && kv[1] != null) {
					String key = kv[0].trim();
					String value = kv[1].trim();
					// 跳过空key
					if (!key.isBlank()) {
						argsMap.put(key, value);
					}
				}
			}
		}
		return argsMap;
	}

	private void scanPackageClasses() {
		if (ResourceLoader.getResource(META_INF_BEANS_INDEX) != null) {
			scanIndex();
		} else {
			scanPackage();
		}
		shutdownThreadPool();
	}

	private void scanIndex() {
		List<String> classNameList = readBeanIndex();
		doLoadClasses(classNameList);
	}

	private void doLoadClasses(List<String> classNameList) {
		if (classNameList.size() > 100) {
			parallelLoadClasses(classNameList);
		} else {
			loadClasses(classNameList);
		}
	}

	private void loadClasses(List<String> classNameList) {
		for (String className : classNameList) {
			try {
				// 核心：加载类
				Class<?> clazz = Class.forName(className, false, ResourceLoader.getClassLoader());
				// 放进当前分片集合
				applicationClasses.add(clazz);
			} catch (Exception e) {
				throw IocException.of(e);
			}
		}

	}

	private void parallelLoadClasses(List<String> classNameList) {
		// 1. 按照线程数分片
		List<List<String>> splitList = splitList(classNameList, THREAD);
		// 2. 每一片提交异步任务，批量加载，返回当前片的List<Class<?>>
		List<CompletableFuture<List<Class<?>>>> futures = new ArrayList<>();
		for (List<String> batch : splitList) {
			CompletableFuture<List<Class<?>>> future = CompletableFuture.supplyAsync(() -> {
				// 每一个分片 自己的集合
				List<Class<?>> partClassList = new ArrayList<>();
				for (String className : batch) {
					try {
						// 核心：加载类
						Class<?> clazz = Class.forName(className, false, ResourceLoader.getClassLoader());
						// 放进当前分片集合
						partClassList.add(clazz);
					} catch (Exception e) {
						throw IocException.of(e);
					}
				}
				// 返回当前分片所有Class
				return partClassList;
			}, ThreadPool);
			futures.add(future);
		}

		// 3. 等待所有任务执行完
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[splitList.size()])).join();

		// 4. 最终总集合，收集所有Class
		for (CompletableFuture<List<Class<?>>> future : futures) {
			try {
				// 把每一片的Class 全部合并到总List
				applicationClasses.addAll(future.get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void loadApplicationClasses(List<String> classNameList) {
		for (String className : classNameList) {
			try (InputStream inputStream = getClassInputStream(className)) {
				if (isAnnotationClass(inputStream)) { // 替代isAnnotationClass
					Class<?> clazz = Class.forName(className, false, ResourceLoader.getClassLoader());
					applicationClasses.add(clazz);
				}
			} catch (Exception e) {
				throw IocException.of(e);
			}
		}

	}

	private InputStream getClassInputStream(String className) throws Exception {

		// 2. 转成类路径格式
		String classPath = className.replace('.', '/') + DOT_CLASS;

		// 4. 打开流
		try (InputStream inputStream = ResourceLoader.getResourceAsStream(classPath)) {
			// 关键：找不到直接抛异常，不返回null
			if (inputStream == null) {
				throw new ClassNotFoundException("未找到类文件：" + className);
			}

			return inputStream;
		}
	}

	private void parallelLoadApplicationClasses(List<String> classNameList) {
		// 1. 按照线程数分片
		List<List<String>> splitList = splitList(classNameList, THREAD);
		// 2. 每一片提交异步任务，批量加载，返回当前片的List<Class<?>>
		List<CompletableFuture<List<Class<?>>>> futures = new ArrayList<>();
		for (List<String> batch : splitList) {
			CompletableFuture<List<Class<?>>> future = CompletableFuture.supplyAsync(() -> {
				// 每一个分片 自己的集合
				List<Class<?>> partClassList = new ArrayList<>();
				for (String className : batch) {

					try (InputStream inputStream = getClassInputStream(className)) {
						if (isAnnotationClass(inputStream)) { // 替代isAnnotationClass
							Class<?> clazz = Class.forName(className, false, ResourceLoader.getClassLoader());
							partClassList.add(clazz);
						}
					} catch (Exception e) {
						throw IocException.of(e);
					}

				}
				// 返回当前分片所有Class
				return partClassList;
			}, ThreadPool);
			futures.add(future);
		}

		// 3. 等待所有任务执行完
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[splitList.size()])).join();

		// 4. 最终总集合，收集所有Class
		for (CompletableFuture<List<Class<?>>> future : futures) {
			try {
				// 把每一片的Class 全部合并到总List
				applicationClasses.addAll(future.get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 列表均匀分片
	 */
	private List<List<String>> splitList(List<String> source, int part) {
		List<List<String>> result = new ArrayList<>();
		int total = source.size();
		int step = (total + part - 1) / part;

		for (int i = 0; i < part; i++) {
			int start = i * step;
			int end = Math.min(start + step, total);
			if (start >= end)
				break;
			result.add(source.subList(start, end));
		}
		return result;
	}

	/**
	 * 关闭线程池
	 */
	private void shutdownThreadPool() {
		ThreadPool.shutdown();
		ThreadPool = null;
	}

	private void scanPackage() {
		List<String> classNameList = scanPackageclassNames();
		doLoadApplicationClasses(classNameList);

	}

	private void doLoadApplicationClasses(List<String> classNameList) {
		if (classNameList.size() > 100) {
			parallelLoadApplicationClasses(classNameList);
		} else {
			loadApplicationClasses(classNameList);
		}

	}

	private List<String> scanPackageclassNames() {
		List<String> classNameList = scanPackageclassNames();
		String pkg = maincClass.getPackageName();
		String path = pkg.replace(POINT, '/');
		try {
			Enumeration<URL> urls = ResourceLoader.getResources(path);
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				if (FILE.equals(url.getProtocol())) {
					// 本地文件系统：只遍历文件名，不读字节
					scanDirByFileName(new File(url.getFile()), pkg, classNameList);
				} else if (JAR.equals(url.getProtocol())) {
					// 兼容jar包运行场景（之前的代码完全不支持）
					scanJarByFileName(url, pkg, classNameList);
				}
			}
		} catch (Exception e) {
			throw IocException.of(e);
		}
		return classNameList;
	}

	/**
	 * 读取 META-INF/beans.index 返回全类名列表
	 */
	public List<String> readBeanIndex() {

		// 正确！从 ClassLoader 读取，兼容一切环境
		try (InputStream is = ResourceLoader.getResourceAsStream(META_INF_BEANS_INDEX)) {
			// 一次性读完，最快、最少IO、最小内存
			byte[] bytes = is.readAllBytes();

			List<String> classNames = new ArrayList<>(1024); // 预指定容量，零扩容
			int lineStart = 0;

			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] == '\n') {
					// 直接截字节，不构造大String，最快
					String line = new String(bytes, lineStart, i - lineStart, StandardCharsets.UTF_8);
					classNames.add(line.strip()); // 只去空白，不做多余处理
					lineStart = i + 1;
				}
			}
			return classNames;

		} catch (IOException e) {
			throw IocException.of(e);
		}
	}

	private void scanJarByFileName(URL url, String pkg, List<String> classNameList) {
		try {
			JarURLConnection jarConn = (JarURLConnection) url.openConnection();
			try (JarFile jarFile = jarConn.getJarFile()) {
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();
					// 只处理目标包下的.class文件
					if (entryName.startsWith(pkg) && entryName.endsWith(DOT_CLASS) && !entryName.contains("$")
							&& !entryName.endsWith(PACKAGE_INFO_CLASS)) {
						// 拼接全类名
						String className = entryName.replace('/', '.').replace(DOT_CLASS, EMPTY);
						classNameList.add(className);

					}
				}
			} catch (IOException e) {
				throw IocException.of(e);
			}
		} catch (Exception e) {
			throw IocException.of(e);
		}

	}

	private void scanDirByFileName(File dir, String pkg, List<String> classNameList) {
		if (!dir.isDirectory()) {
			return;
		}
		try (Stream<Path> pathStream = Files.walk(dir.toPath())) {
			pathStream
					// 只处理.class文件
					.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(DOT_CLASS))
					// 过滤内部类/无用类
					.filter(p -> !p.getFileName().toString().contains("$"))
					.filter(p -> !p.getFileName().toString().startsWith(PACKAGE_INFO)).forEach(p -> {
						// 拼接全类名（核心：只拼名字，不读文件）
						String relativePath = dir.toPath().relativize(p).toString();
						String className = pkg + "."
								+ relativePath.replace(File.separatorChar, '.').replace(DOT_CLASS, EMPTY);
						classNameList.add(className);
					});
		} catch (IOException e) {
			throw IocException.of(e);
		}

	}

	private boolean isAnnotationClass(InputStream inputStream) {
		for (Class<? extends Annotation> anno : beanAnnotationClasses) {
			if (AnnotationUtils.hasAnnotation(inputStream, anno)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAnnotationClass(Class<?> clazz) {
		for (Class<? extends Annotation> anno : beanAnnotationClasses) {
			if (clazz.isAnnotationPresent(anno)) {
				return true;
			}
		}
		return false;
	}

	private void loadPlugin() {
		ClassLoader classLoader = ResourceLoader.getClassLoader();
		ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, classLoader);
		for (Plugin plugin : loader) {
			plugins.add(plugin);
		}
	}

	private void registerBeanDefinitions() {
		for (Class<?> clazz : applicationClasses) {
			registerBeanDefinition(clazz);
		}
		for (Class<?> clazz : pluginClasses) {
			registerBeanDefinition(clazz);
		}
	}

	private void registerBeanDefinition(Class<?> clazz) {
		if (clazz.isInterface()) {
			for (BeanFactoryAnnotationMatcher factoryBeanMatcher : beanFactoryAnnotationMatchers) {
				if (clazz.isAnnotationPresent(factoryBeanMatcher.getBeanAnnotationClass())) {
					boolean eager = getEager(clazz);
					beanDefinitionMap.putIfAbsent(clazz,
							new InterfaceBeanDefinition(clazz, factoryBeanMatcher.getBeanFactoryClass(), eager));
					beanClassSet.add(clazz);
				}
			}
		}
		if (clazz.isAnnotationPresent(Component.class) && isNormalClass(clazz)) {
			boolean eager = getEager(clazz);
			beanDefinitionMap.putIfAbsent(clazz, new ClassBeanDefinition(clazz, eager));
			beanClassSet.add(clazz);
		}
		if (clazz.isAnnotationPresent(ConfigurationProperties.class) && isNormalClass(clazz)) {
			beanDefinitionMap.putIfAbsent(clazz, new PropertiesBeanDefinition(clazz, false));
			beanClassSet.add(clazz);
		}
		if (clazz.isAnnotationPresent(Configuration.class) && isNormalClass(clazz)) {
			if (hasCondition(clazz)) {
				if (isConditionTrue(clazz)) {
					registerConfigurationBeanDefinition(clazz);
				}
			} else {
				registerConfigurationBeanDefinition(clazz);
			}
		}
		if (isAnnotationClass(clazz)) {
			for (BeanFactoryAnnotationMatcher factoryBeanMatcher : beanFactoryAnnotationMatchers) {
				if (clazz.isAnnotationPresent(factoryBeanMatcher.getBeanAnnotationClass())) {
					boolean eager = getEager(clazz);
					beanDefinitionMap.putIfAbsent(clazz, new ClassBeanDefinition(clazz, eager));
					beanClassSet.add(clazz);
				}
			}
		}
	}

	private void registerConfigurationBeanDefinition(Class<?> clazz) {
		for (Method beanMethod : clazz.getDeclaredMethods()) {
			if (hasCondition(beanMethod)) {
				if (isConditionTrue(beanMethod)) {
					registerBeanDefinition(clazz, beanMethod);
				}
			} else {
				registerBeanDefinition(clazz, beanMethod);
			}
		}
	}

	private void registerBeanDefinition(Class<?> clazz, Method beanMethod) {
		if (beanMethod.isAnnotationPresent(Bean.class)) {
			boolean eagerMethod = getEager(beanMethod);
			Class<?> returnType = beanMethod.getReturnType();
			beanDefinitionMap.putIfAbsent(returnType, new MethodBeanDefinition(clazz, beanMethod, eagerMethod));
			beanClassSet.add(returnType);
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

	private void instantiateEagerBeans() {
		for (Entry<Class<?>, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
			Class<?> key = entry.getKey();
			BeanDefinition value = entry.getValue();
			if (value.isEager()) {
				getBean(key);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clazz) {
		Object bean = singletonPool.get(clazz);
		if (bean != null) {
			return (T) bean;
		}
		synchronized (singletonPool) {
			if (bean == null) {
				bean = checkAndCreateBean(clazz);
				singletonPool.put(clazz, bean);
			}
		}
		return (T) bean;
	}

	@SuppressWarnings("unchecked")
	private <T> T checkAndCreateBean(Class<T> clazz) {
		if (!isUsableBeanClass(clazz)) {
			throw IocException.of("Class must be interface or instantiable class:" + clazz.getName());
		}
		Class<?> targetClass = clazz;
		if (clazz.isInterface()) {
			List<Class<?>> keys = getImplementationClasses(clazz);
			if (keys.isEmpty()) {
				throw IocException.of("No such BeanDefinition:" + clazz.getName());
			} else if (keys.size() > 1) {
				throw IocException.of("Too Many BeanDefinition");
			} else {
				targetClass = keys.get(0);
			}
		}
		Object bean = createBean(targetClass);
		return (T) bean;

	}

	private boolean isUsableBeanClass(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		if (clazz.isInterface()) {
			return true;
		}
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isEnum()
				&& !clazz.isAnnotation() && !clazz.isArray() && !clazz.isPrimitive() && clazz != void.class
				&& clazz != Void.class;
	}

	private Object createBean(Class<?> clazz) {
		try {
			BeanDefinition bd = beanDefinitionMap.get(clazz);
			if (bd == null) {
				throw IocException.of("No such BeanDefinition:" + clazz.getName());
			}
			Object instance = null;
			if (bd.beanDefinitionType() == BeanDefinitionType.PROPERTIES) {
				PropertiesBeanDefinition propertiesBeanDefinition = bd.wrap();
				Class<?> beanClass = propertiesBeanDefinition.getBeanClass();
				instance = createPropertiesBean(beanClass);
				return instance;
			}
			if (bd.beanDefinitionType() == BeanDefinitionType.CLASS) {
				ClassBeanDefinition classBeanDefinition = bd.wrap();
				Class<?> beanClass = classBeanDefinition.getBeanClass();
				instance = createClassBean(beanClass);
			}
			if (bd.beanDefinitionType() == BeanDefinitionType.METHOD) {
				MethodBeanDefinition methodBeanDefinition = bd.wrap();
				Class<?> configurationClass = methodBeanDefinition.getConfigurationClass();
				Method beanMethod = methodBeanDefinition.getBeanMethod();
				instance = createMethodBean(configurationClass, beanMethod);
			}
			if (bd.beanDefinitionType() == BeanDefinitionType.INTERFACE) {
				InterfaceBeanDefinition interfaceBeanDefinition = bd.wrap();
				Class<?> beanClass = interfaceBeanDefinition.getBeanClass();
				Class<? extends BeanFactory> beanFactoryClass = interfaceBeanDefinition.getBeanFactory();
				instance = createInterfaceBean(beanClass, beanFactoryClass);
			}
			for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
				if (beanPostProcessor.matche(clazz)) {
					instance = beanPostProcessor.postProcess(this, instance);
				}
			}
			ProxyContext.bind(clazz, instance);
			return instance;
		} catch (Exception e) {
			throw IocException.of(e);
		}
	}

	private Object createInterfaceBean(Class<?> beanClass, Class<? extends BeanFactory> beanFactoryClass) {
		Object instance;
		BeanFactory beanFactoryInstance = getBeanFactory(beanFactoryClass);
		instance = beanFactoryInstance.createBean(this, beanClass);
		return instance;
	}

	private Object createMethodBean(Class<?> configurationClass, Method beanMethod)
			throws IllegalAccessException, InvocationTargetException {
		Object instance;
		Object configurationInstance = getConfiguration(configurationClass);
		Class<?>[] types = beanMethod.getParameterTypes();
		Object[] methodArgs = resolveArgs(types);
		instance = beanMethod.invoke(configurationInstance, methodArgs);
		return instance;
	}

	private Object createClassBean(Class<?> beanClass)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Constructor<?> ctor = beanClass.getConstructors()[0];
		Object[] args = resolveArgs(ctor.getParameterTypes());
		Object instance = ctor.newInstance(args);
		return instance;

	}

	private Object createPropertiesBean(Class<?> beanClass) {
		ConfigurationProperties configurationProperties = beanClass.getAnnotation(ConfigurationProperties.class);
		String prefix = configurationProperties.prefix();
		return environment.getProperty(prefix, beanClass);
	}

	private void instantiateBeanPostProcessors() {
		for (Class<?> bbpClass : beanPostProcessorClasses) {
			Constructor<?> ctor = bbpClass.getConstructors()[0];
			try {
				BeanPostProcessor beanPostProcessor = (BeanPostProcessor) ctor.newInstance();
				beanPostProcessors.add(beanPostProcessor);
			} catch (Exception e) {
				throw IocException.of(e);
			}
		}
		beanPostProcessors.sort(Comparator.comparingInt(BeanPostProcessor::getOrder));
	}

	private BeanFactory getBeanFactory(Class<? extends BeanFactory> beanFactoryClass) {
		return beanFactoryMap.computeIfAbsent(beanFactoryClass, func -> {
			try {
				return beanFactoryClass.getConstructor().newInstance();
			} catch (Exception e) {
				throw IocException.of(e);
			}
		});
	}

	private Object getConfiguration(Class<?> configurationClass) {
		return configurationMap.computeIfAbsent(configurationClass, func -> {
			try {
				Constructor<?> ctor = configurationClass.getConstructors()[0];
				Object[] args = resolveArgs(ctor.getParameterTypes());
				return ctor.newInstance(args);
			} catch (Exception e) {
				throw IocException.of(e);
			}
		});
	}

	private Object[] resolveArgs(Class<?>[] types) {
		Object[] args = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			args[i] = getBean(types[i]);
		}
		return args;
	}

	private boolean isNormalClass(Class<?> clazz) {
		return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isEnum()
				&& !clazz.isAnnotation();
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
		return ResourceLoader.getResource(resourceName) != null;
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

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getBeansOfType(Class<T> interfaceClass) {
		if (!interfaceClass.isInterface()) {
			throw IocException.of("Class must be interface:" + interfaceClass.getName());
		}
		List<Class<?>> keys = getImplementationClasses(interfaceClass);
		List<T> list = new ArrayList<>();
		for (Class<?> key : keys) {
			list.add((T) getBean(key));
		}
		return list;
	}

	private boolean hasCondition(AnnotatedElement annotatedElement) {
		if (annotatedElement.isAnnotationPresent(ConditionalOnClass.class)) {
			return true;
		}
		if (annotatedElement.isAnnotationPresent(ConditionalOnMissingBean.class)) {
			return true;
		}
		if (annotatedElement.isAnnotationPresent(ConditionalOnPropertity.class)) {
			return true;
		}
		return false;
	}

	private boolean isConditionTrue(Class<?> clazz) {

		if (clazz.isAnnotationPresent(ConditionalOnClass.class)) {
			ConditionalOnClass conditionalOnClass = clazz.getAnnotation(ConditionalOnClass.class);
			String className = conditionalOnClass.className();
			if (!isClassPresent(className)) {
				return false;
			}
			if (clazz.isAnnotationPresent(ConditionalOnPropertity.class)) {
				ConditionalOnPropertity conditionalOnPropertity = clazz.getAnnotation(ConditionalOnPropertity.class);
				String key = conditionalOnPropertity.key();
				String value = conditionalOnPropertity.value();
				if (!matchesProperty(env, key, value)) {
					return false;
				}
			}
			if (clazz.isAnnotationPresent(ConditionalOnMissingBean.class)) {
				if (clazz.isInterface()) {
					List<Class<?>> classes = getImplementationClasses(clazz);
					if (!classes.isEmpty()) {
						return false;
					}
				} else {
					BeanDefinition beanDefinition = beanDefinitionMap.get(clazz);
					if (!Objects.isNull(beanDefinition)) {
						return false;
					}
				}

			}
		}
		return true;
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
				List<Class<?>> classes = getImplementationClasses(key);
				if (!classes.isEmpty()) {
					return false;
				}
			} else {
				BeanDefinition beanDefinition = beanDefinitionMap.get(key);
				if (!Objects.isNull(beanDefinition)) {
					return false;
				}
			}
		}
		return true;
	}

	private List<Class<?>> getImplementationClasses(Class<?> clazz) {
		List<Class<?>> keys = new ArrayList<>();
		for (Class<?> key : beanClassSet) {
			if (clazz.isAssignableFrom(key)) {
				keys.add(key);
			}
		}
		return keys;
	}

	@Override
	public Set<Class<?>> getBeanClasses() {
		return beanClassSet;
	}

	@Override
	public Environment getEnvironment() {
		return environment;
	}

}