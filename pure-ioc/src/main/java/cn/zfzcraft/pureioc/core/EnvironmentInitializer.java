package cn.zfzcraft.pureioc.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import cn.zfzcraft.pureioc.core.exception.IgnoreException;
import cn.zfzcraft.pureioc.utils.NestedMapUtils;
import cn.zfzcraft.pureioc.utils.ResourceUtils;

public class EnvironmentInitializer {

	private static final String ENV = "env";

	private static final String EMPTY = "";

	private static final String LINE = "-";

	private static final String DOT_YML = ".yml";

	private static final String APP = "app";

	private static final String APP_YML = "app.yml";

	private static final Yaml YAML = new Yaml();

	public static Map<String, Object> initializeEnvironment(String[] args) {
		Map<String, Object> env = new HashMap<>();
		// 1. 加载基础配置 app.yml
		Map<String, Object> ymlConfig = loadYamlResource(APP_YML);
		env.putAll(ymlConfig);
		// 启动参数扁平化map
		Map<String, String> argsMap = parseMainArguments(args);
		// 转嵌套map
		Map<String, Object> nestedArgsMap = flatMapToNestedMap(argsMap);
		// 启动参数覆盖所有配置
		deepMerge(env, nestedArgsMap);
		// 确定激活的环境（命令行优先，其次是配置文件）
		String activeProfile = determineActiveProfile(env);
		// 加载并合并环境配置 application-{active}.yml
		if (isNotEmpty(activeProfile)) {
			String envConfigFile = APP + LINE + activeProfile + DOT_YML;
			Map<String, Object> envConfig = loadYamlResource(envConfigFile);
			// 活动参数覆盖所有配置
			deepMerge(env, envConfig);
			// 启动参数覆盖所有配置（最高优先级）
			deepMerge(env, nestedArgsMap);
		}
		return env;
	}

	private static boolean isNotEmpty(String activeProfile) {
		return activeProfile != null && activeProfile != EMPTY;
	}

	/**
	 * 把 k=v 格式的启动参数转换成 嵌套 Map<String, Object> 支持： key=value a.b.c=123 arr[0]=aaa
	 * arr[1]=bbb
	 */
	private static Map<String, Object> flatMapToNestedMap(Map<String, String> args) {
		Map<String, Object> root = new HashMap<>();
		for (Map.Entry<String, String> entry : args.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			put(root, key, value);
		}
		return root;
	}

	@SuppressWarnings("unchecked")
	private static void put(Map<String, Object> root, String key, String value) {
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
	private static <T> T getOrCreate(Map<String, Object> map, String key, Class<T> type) {
		Object obj = map.get(key);
		if (obj == null) {
			obj = type == Map.class ? new HashMap<String, Object>() : new ArrayList<>();
			map.put(key, obj);
		}
		return (T) obj;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map<String, Object> getOrCreate(List<Object> list, int index, Class<Map> type) {
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

	private static String determineActiveProfile(Map<String, Object> env) {
		EnvironmentProperties environmentProperties = NestedMapUtils.loadAs(env, ENV, EnvironmentProperties.class);
		String active = environmentProperties.getActive();
		return active == null ? null : active.trim();
	}

	/**
	 * 从类路径加载YAML文件，返回嵌套Map（空安全）
	 */
	private static Map<String, Object> loadYamlResource(String fileName) {
		try (InputStream inputStream = ResourceUtils.getResourceAsStream(fileName)) {
			// 文件不存在返回空Map
			if (inputStream == null) {
				return new LinkedHashMap<>();
			}
			// 解析YAML（SnakeYAML返回null表示空文件）
			Map<String, Object> yamlMap = YAML.load(inputStream);
			return yamlMap == null ? new LinkedHashMap<>() : yamlMap;
		} catch (Exception e) {
			throw new IgnoreException("ignore", e);
		}
	}

	/**
	 * 解析main启动参数：--key=value → 扁平Map
	 */
	private static Map<String, String> parseMainArguments(String[] args) {
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

	/**
	 * 深度合并两个嵌套Map： 1. Map类型递归合并 2. List类型追加合并 3. 基础类型直接覆盖
	 */
	@SuppressWarnings("unchecked")
	public static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
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
}
