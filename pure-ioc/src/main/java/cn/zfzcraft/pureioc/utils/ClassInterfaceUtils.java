package cn.zfzcraft.pureioc.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * 递归收集类所有接口（含父类、父接口）
 * 排除 JDK 内置无用标记接口
 * 判断最终是否只有 1 个业务接口
 */
public class ClassInterfaceUtils {

    // 排除 JDK 自带的标记接口、无业务意义的接口
    private static final Set<String> EXCLUDE_INTERFACES = Set.of(
            "java.io.Closeable",
            "java.lang.AutoCloseable",
            "java.io.Serializable",
            "java.lang.Cloneable",
            "java.lang.Comparable"
    );

    /**
     * 判断类【递归所有层级、去重、排除JDK接口后】是否只有 1 个业务接口
     */
    public static boolean hasOnlyOneBusinessInterface(Class<?> clazz) {
        Set<Class<?>> allInterfaces = new HashSet<>();
        collectAllInterfaces(clazz, allInterfaces);

        // 最终剩下的就是真实业务接口
        return allInterfaces.size() == 1;
    }

    /**
     * 递归收集：当前类 + 所有父类 + 所有父接口
     */
    private static void collectAllInterfaces(Class<?> clazz, Set<Class<?>> result) {
        if (clazz == null || clazz == Object.class) {
            return;
        }

        // 遍历当前类实现的所有接口
        for (Class<?> itf : clazz.getInterfaces()) {
            // 排除 JDK 无用接口
            if (!EXCLUDE_INTERFACES.contains(itf.getName())) {
                result.add(itf);
            }
            // 递归收集接口的父接口
            collectAllInterfaces(itf, result);
        }

        // 递归父类
        collectAllInterfaces(clazz.getSuperclass(), result);
    }

	public static <T>  Set<Class<?>> getAllBusinessInterfaces(Class<T> type) {
		Set<Class<?>> allInterfaces = new HashSet<>();
        collectAllInterfaces(type, allInterfaces);

        // 最终剩下的就是真实业务接口
        return allInterfaces;
	}
}