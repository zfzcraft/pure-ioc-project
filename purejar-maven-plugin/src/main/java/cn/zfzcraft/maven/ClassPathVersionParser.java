package cn.zfzcraft.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析包含JDK版本信息的类路径工具类
 * 支持解析如：META-INF/versions/9/org/yaml/snakeyaml/internal/Logger$Level.class 格式的字符串
 */
public class ClassPathVersionParser {

    // 正则表达式：匹配 META-INF/versions/[版本号]/[类路径].class 格式
    private static final Pattern VERSION_CLASS_PATTERN = 
            Pattern.compile("META-INF/versions/(\\d+)/(.+)\\.class");

    /**
     * 解析类路径字符串，提取JDK版本号和完整类名
     * @param classPath 待解析的类路径字符串（如：META-INF/versions/9/org/yaml/snakeyaml/internal/Logger.class）
     * @return 解析结果对象（包含版本号和类名），解析失败返回null
     */
    public static ClassVersionInfo parse(String classPath) {
        // 空值校验
        if (classPath == null || classPath.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = VERSION_CLASS_PATTERN.matcher(classPath.trim());
        if (matcher.find()) {
            // 提取JDK版本号（分组1）
            int jdkVersion = Integer.parseInt(matcher.group(1));
            // 提取类路径（分组2），并将/替换为.，$保留（内部类标识）
            String className = matcher.group(2).replace('/', '.');
            
            return new ClassVersionInfo(jdkVersion, className);
        }
        return null;
    }

    /**
     * 内部静态类：存储解析后的版本号和类名
     */
    public static class ClassVersionInfo {
        private final int jdkVersion;    // JDK版本号（如9、11、17）
        private final String className;  // 完整类名（如org.yaml.snakeyaml.internal.Logger$Level）

        public ClassVersionInfo(int jdkVersion, String className) {
            this.jdkVersion = jdkVersion;
            this.className = className;
        }

        // Getter方法
        public int getJdkVersion() {
            return jdkVersion;
        }

        public String getClassName() {
            return className;
        }

        // 重写toString，方便打印查看
        @Override
        public String toString() {
            return "JDK版本：" + jdkVersion + "，类名：" + className;
        }
    }

}
