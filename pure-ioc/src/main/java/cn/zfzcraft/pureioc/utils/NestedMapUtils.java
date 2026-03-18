package cn.zfzcraft.pureioc.utils;

import java.util.Map;

import org.yaml.snakeyaml.Yaml;


public class NestedMapUtils {
	
	private static final Yaml YAML = new Yaml();
	/**
	 * 从嵌套 Map 中按 a.b.c 取最终叶子值
	 */
	public static Object getNestedValue(Map<String, Object> rootMap, String key) {
		String[] paths = key.split("\\.");
		Object current = rootMap;

		for (String path : paths) {
			if (!(current instanceof Map)) {
				return null;
			}
			current = ((Map<?, ?>) current).get(path);
			if (current == null) {
				return null;
			}
		}
		return current;
	}
	
	
	public static <T> T loadAs(Map<String, Object> root, String prefix, Class<T> clazz) {

		// 2. 按 . 路径递归取值
		Map<String, Object> subMap = getNestedMap(root, prefix);

		// 3. 将子 Map 转成目标对象
		return YAML.loadAs(YAML.dump(subMap), clazz);
	}
	
	/**
	 * 按 a.b.c 从根 Map 中获取嵌套 Map
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getNestedMap(Map<String, Object> root, String path) {
		String[] keys = path.split("\\.");
		Map<String, Object> current = root;

		for (int i = 0; i < keys.length - 1; i++) {
			current = (Map<String, Object>) current.get(keys[i]);
		}

		// 最后一级也返回 Map
		return (Map<String, Object>) current.get(keys[keys.length - 1]);
	}
}
