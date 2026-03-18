package cn.zfzcraft.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义FatJar类加载器
 * 核心规则：
 * 1. 严格遵循JDK双亲委派规范，仅重写标准扩展点findXXX
 * 2. 资源全量委托父加载器，仅做路径映射
 * 3. MR多版本类初始化时仅保留当前JDK版本有效条目，零冗余
 * 4. 普通类固定走classes/目录，职责边界100%隔离
 * 5. 不创建JarFile，所有流从父加载器获取
 * 6. 内存友好，无无用对象，线程安全
 *
 * @author PureJar Framework
 * @since JDK 17
 */
public final class JarClassLoader extends ClassLoader {

    private static final String CLASSES_ROOT = "classes/";
	private static final String DOT_CLASS = ".class";
	private static final String META_INF_RESOURCES_INDEX = "META-INF/resources.index";
	private static final String META_INF_MRJAR_INDEX = "META-INF/mrjars.index";
	private static final String KEY_VALUE_SEPARATOR = "=";
	private static final String PREFIX = "#";
	// 资源索引：逻辑名 -> 真实路径列表（按优先级排序），不可变
    private final Map<String, List<String>> resourceIndex;
    // MR类索引：全类名 -> 当前JDK版本对应class路径，不可变，零冗余
    private final Map<String, String> mrClassIndex;
    // 固定保护域，保证权限一致性，避免重复创建
    private final ProtectionDomain protectionDomain;
    
    private final ClassLoader resourceLoader;

    /**
     * 构造器：一次性完成所有初始化，无运行期额外开销
     * @param parent 父类加载器，不可为空
     * @throws IllegalStateException 索引解析失败时抛出
     */
    public JarClassLoader(ClassLoader parent,ClassLoader resourceLoader) {
        super(parent);
        this.resourceLoader = resourceLoader;
        // 仅构造期使用的局部变量，不占用长期内存
        final int currentJdkVersion = Runtime.version().feature();
        this.protectionDomain = initProtectionDomain();
        this.resourceIndex = loadResourceIndex();
        this.mrClassIndex = loadMrClassIndex(currentJdkVersion);
    }

    // ====================== 核心类加载（仅重写JDK标准扩展点） ======================
    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        // 严格类名校验，防止路径遍历与注入攻击
        if (!isValidClassName(className)) {
            throw new ClassNotFoundException("非法类名: " + className);
        }
        // 1. 优先匹配MR类（仅当前JDK版本有效条目，O(1)查找）
        String classPath = mrClassIndex.get(className);
        // 2. 普通类走固定路径
        if (classPath == null) {
            classPath = CLASSES_ROOT + className.replace('.', '/') + DOT_CLASS;
        }
        // 3. 从父加载器获取字节码，自行定义类
        byte[] classBytes;
        try {
            classBytes = readClassBytes(classPath);
        } catch (IOException e) {
            throw new ClassNotFoundException("类加载失败: " + className + ", 路径: " + classPath, e);
        }
        Class<?>  clazz =defineClass(className, classBytes, 0, classBytes.length, protectionDomain);
        return clazz;
    }

    // ====================== 核心资源加载（仅重写JDK标准扩展点） ======================
    @Override
    protected URL findResource(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // 按优先级顺序查找，返回第一个匹配资源
        for (String path : resourceIndex.getOrDefault(name, List.of())) {
            URL url = resourceLoader.getResource(path);
            if (url != null) {
            	System.out.println(url);
                return url;
            }
        }
		return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        if (name == null || name.isBlank()) {
            return Collections.emptyEnumeration();
        }
        // 全量收集匹配资源，保证SPI等场景完整性
        List<URL> matchedUrls = new ArrayList<>();
        for (String path : resourceIndex.getOrDefault(name, List.of())) {
            URL url = resourceLoader.getResource(path);
                matchedUrls.add(url);
        }
        return Collections.enumeration(matchedUrls);
    }

    // ====================== 私有辅助方法（final不可重写，保证安全） ======================
    private ProtectionDomain initProtectionDomain() {
        CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
        return new ProtectionDomain(codeSource, null, this, null);
    }

    /**
     * 加载并解析资源索引，无冗余数据
     */
    private Map<String, List<String>> loadResourceIndex() {
        final String indexPath = META_INF_RESOURCES_INDEX;
        try (InputStream in = resourceLoader.getResourceAsStream(indexPath)) {
            // 索引文件不存在直接返回空Map，不报错
            if (in == null) {
                return Map.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                Map<String, List<String>> index = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith(PREFIX))
                        .collect(Collectors.toMap(
                                line -> line.split(KEY_VALUE_SEPARATOR, 2)[0].trim(),
                                line -> Arrays.stream(line.split(KEY_VALUE_SEPARATOR, 2)[1].split(","))
                                        .map(String::trim)
                                        .filter(p -> !p.isBlank())
                                        .toList(),
                                (oldVal, newVal) -> newVal
                        ));
                return Map.copyOf(index);
            }
        } catch (Exception e) {
            throw new IllegalStateException("资源索引解析失败: " + indexPath, e);
        }
    }

    /**
     * 加载并解析MR索引，初始化时直接过滤非当前版本条目，零冗余
     */
    private Map<String, String> loadMrClassIndex(int currentJdkVersion) {
        final String indexPath = META_INF_MRJAR_INDEX;
        try (InputStream in = resourceLoader.getResourceAsStream(indexPath)) {
            // 索引文件不存在直接返回空Map，不报错
            if (in == null) {
                return Map.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                Map<String, String> index = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith(PREFIX))
                        .map(line -> line.split(KEY_VALUE_SEPARATOR, 3))
                        .filter(parts -> parts.length == 3)
                        // 非当前版本条目直接丢弃，完全不进入内存
                        .filter(parts -> Integer.parseInt(parts[1].trim()) == currentJdkVersion)
                        .collect(Collectors.toMap(
                                parts -> parts[0].trim(),
                                parts -> parts[2].trim(),
                                (oldVal, newVal) -> newVal
                        ));
                return Map.copyOf(index);
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException("MR索引版本号格式错误: " + indexPath, e);
        } catch (Exception e) {
            throw new IllegalStateException("MR索引解析失败: " + indexPath, e);
        }
    }

    /**
     * 从父加载器读取类字节码
     */
    private byte[] readClassBytes(String classPath) throws IOException {
        try (InputStream in = resourceLoader.getResourceAsStream(classPath)) {
            if (in == null) {
                throw new IOException("类路径不存在");
            }
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                throw new IOException("类字节码为空");
            }
            return bytes;
        }
    }

    /**
     * 严格类名校验，防止路径遍历与非法字符
     */
    private boolean isValidClassName(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        // 禁止路径遍历、空字符、非法分隔符
        return !className.contains("..")
                && !className.contains("\0")
                && !className.contains("/")
                && !className.contains("\\");
    }

    /**
     * 调试友好的toString，无敏感信息，方便问题排查
     */
    @Override
    public String toString() {
        return "JarClassLoader{" +
                "parent=" + getParent() +
                ", resourceCount=" + resourceIndex.size() +
                ", mrClassCount=" + mrClassIndex.size() +
                '}';
    }
}