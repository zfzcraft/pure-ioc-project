package cn.zfzcraft.pureioc.utils;


import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDK动态代理反射优化工具
 * 替代传统Method.invoke()，基于LambdaMetafactory生成原生调用器，性能接近直接调用
 *
 * @author 自定义优化工具
 * @date 2026-03-27
 */
public class ProxyReflectUtils {

    // 函数式接口：通用方法调用器，替代Method.invoke
    @FunctionalInterface
    public interface MethodInvoker {
        /**
         * 执行目标方法调用
         * @param target 目标对象（静态方法传null）
         * @param args 方法参数
         * @return 方法返回值
         * @throws Throwable 方法执行异常直接透传
         */
        Object invoke(Object target, Object[] args) throws Throwable;
    }

    // 线程安全缓存：Method -> 对应的MethodInvoker，原子性操作避免重复生成
    private static final Map<Method, MethodInvoker> INVOKER_CACHE = new ConcurrentHashMap<>();

    // Lambda元工厂常量
    private static final String SAM_METHOD_NAME = "invoke";
    // SAM接口方法的固定类型：(Object target, Object[] args) -> Object
    private static final MethodType SAM_METHOD_TYPE = MethodType.methodType(Object.class, Object.class, Object[].class);

    /**
     * 获取方法对应的原生调用器（优先从缓存获取）
     * @param method 目标反射方法
     * @return 原生调用器，无反射开销
     */
    public static MethodInvoker getMethodInvoker(Method method) {
        // 原子性操作：缓存命中直接返回，未命中则生成，高并发安全
        return INVOKER_CACHE.computeIfAbsent(method, targetMethod -> {
            try {
                // 1. 获取目标类的全权限Lookup（JDK9+ 标准API）
                MethodHandles.Lookup callerLookup = MethodHandles.lookup();
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetMethod.getDeclaringClass(), callerLookup);

                // 2. 将反射Method转为原始MethodHandle
                MethodHandle originalHandle = lookup.unreflect(targetMethod);
                int paramCount = targetMethod.getParameterCount();

                // 3. 核心修复：链式调用一次性赋值，显式加final，完全满足final要求
                final MethodHandle adaptedHandle = originalHandle
                        .asSpreader(Object[].class, paramCount)
                        .asType(SAM_METHOD_TYPE);

                // 4. 构建Lambda工厂类型：无参构造，返回MethodInvoker实例
                MethodType factoryType = MethodType.methodType(MethodInvoker.class);

                // 5. 调用LambdaMetafactory生成原生调用器，参数严格匹配
                CallSite callSite = LambdaMetafactory.metafactory(
                        lookup,
                        SAM_METHOD_NAME,
                        factoryType,
                        SAM_METHOD_TYPE,
                        adaptedHandle,
                        SAM_METHOD_TYPE
                );

                // 6. 生成MethodInvoker实例
                return (MethodInvoker) callSite.getTarget().invokeExact();
            } catch (Throwable e) {
                // 生成失败兜底：预缓存MethodHandle，性能仍远高于Method.invoke
                return buildFallbackInvoker(targetMethod);
            }
        });
    }

    /**
     * 兜底方案：预缓存MethodHandle的调用器，Lambda生成失败时使用
     */
    private static MethodInvoker buildFallbackInvoker(Method method) {
        try {
            // 预获取全权限MethodHandle，避免每次调用都反射
            MethodHandles.Lookup callerLookup = MethodHandles.lookup();
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), callerLookup);
            MethodHandle originalHandle = lookup.unreflect(method);
            int paramCount = method.getParameterCount();

            // 核心修复：链式调用一次性赋值，显式加final，lambda捕获完全合规
            final MethodHandle adaptedHandle = originalHandle
                    .asSpreader(Object[].class, paramCount)
                    .asType(SAM_METHOD_TYPE);

            // 返回基于MethodHandle的高性能调用器，捕获final变量无编译错误
            return (target, args) -> adaptedHandle.invokeExact(target, args);
        } catch (Throwable e) {
            // 终极兜底：极端场景下用Method.invoke，保证可用性
            return method::invoke;
        }
    }

    /**
     * 对外简化调用方法
     * @param method 目标方法
     * @param target 目标对象
     * @param args 方法参数
     * @return 方法返回值
     * @throws Throwable 执行异常
     */
    public static Object invokeMethod(Method method, Object target, Object[] args) throws Throwable {
        return getMethodInvoker(method).invoke(target, args);
    }

    /**
     * 清空缓存（热更新类时按需调用）
     */
    public static void clearCache() {
        INVOKER_CACHE.clear();
    }
}