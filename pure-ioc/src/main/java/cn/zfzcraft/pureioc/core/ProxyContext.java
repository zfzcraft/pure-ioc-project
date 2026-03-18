package cn.zfzcraft.pureioc.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//动态代理上下文
public final class ProxyContext {

	protected static final Map<Class<?>, Object> PROXY_MAP = new ConcurrentHashMap<>();

    private ProxyContext() {
        throw new AssertionError();
    }

    // 容器内部：注册【最外层代理】
    public static void bind(Class<?> beanType, Object proxy) {
        if (beanType == null || proxy == null) {
            return;
        }
        PROXY_MAP.put(beanType, proxy);
    }

    // 外部获取：自动按继承 / 接口匹配（isAssignableFrom）
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type) {
        if (type == null) {
            return null;
        }

        // 1. 先精确匹配（最快）
        T proxy = (T) PROXY_MAP.get(type);
        if (proxy != null) {
            return proxy;
        }

        // 2. 没有精确匹配，遍历找最匹配的类型
        for (Map.Entry<Class<?>, Object> entry : PROXY_MAP.entrySet()) {
            Class<?> beanType = entry.getKey();
            if (type.isAssignableFrom(beanType)) {
                return (T) entry.getValue();
            }
        }
        return null;
    }

    protected static void clear() {
        PROXY_MAP.clear();
    }
}