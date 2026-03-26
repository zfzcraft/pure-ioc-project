package cn.zfzcraft.pureioc.core;

import java.util.Map;


public interface Environment{
	
	/**
	 * 根据【点分隔路径】获取配置值
	 * <p>
	 * <b>key 格式规则：</b>
	 * 必须使用 {@code '.'} 分隔层级，例如 {@code datasource.url}
	 * <p>
	 * 若中间某一层不是 Map，或路径不存在，返回 null。
	 *
	 * @param key 点分隔的配置路径，如 a.b.c
	 * @return 配置值，不存在则为 null
	 */
	Object getProperty(String key);
	/**
	 * 根据【点分隔路径】获取配置值
	 * <p>
	 * <b>prefix 格式规则：</b>
	 * 必须使用 {@code '.'} 分隔层级，例如 {@code system.datasource}
	 * <p>
	 * 若中间某一层不是 Map，或路径不存在，返回 null。
	 *
	 * @param prefix 点分隔的配置路径，如 a.b.c
	 * @return entity，不存在则为 null
	 */
	<T> T getProperty(String prefix, Class<T> type);
	/**
	 * 向【点分隔路径】设置/覆盖配置值
	 * <p>
	 * <b>key 强制规范：</b>
	 * 必须使用 {@code '.'} 分隔多级结构，例如：
	 * {@code datasource.password}
	 * <p>
	 * 若中间某一层不存在或不是 Map，会自动创建新的 HashMap 补齐结构，
	 * 因此支持直接写入深层路径，无需提前创建父层级。
	 * <p>
	 * 典型用途：
	 * 加密配置解密、动态覆盖、配置后处理。
	 *
	 * @param key   点分隔配置路径
	 * @param value 要设置的目标值
	 */
	void setProperty(String key,Object value);
	
	/**
	 * 向【点分隔路径】设置/覆盖配置值
	 * <p>
	 * <b>key 强制规范：</b>
	 * 必须使用 {@code '.'} 分隔多级结构，例如：
	 * {@code datasource.password}
	 * <p>
	 * 若中间某一层不存在或不是 Map，会自动创建新的 HashMap 补齐结构，
	 * 因此支持直接写入深层路径，无需提前创建父层级。
	 * <p>
	 * 典型用途：
	 * 加密配置解密、动态覆盖、配置后处理。
	 *
	 * @param key   点分隔配置路径
	 * 
	 */
	boolean containsProperty(String key);
	/**
	 * 应用运行时环境配置持有者
	 * <p>
	 * 内部存储为嵌套 Map 结构，与 YAML 层级完全对应；
	 * 对外统一使用【点分隔路径】（如 a.b.c）作为 property key。
	 * <p>
	 * 
	 */
	Map<String, Object> getPropertyMap();
}
