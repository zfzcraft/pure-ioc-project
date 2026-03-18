package cn.zfzcraft.loader;

import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * PureJar 标准启动器
 * 对应 MANIFEST.MF 中的 Main-Class
 * 作用：
 * 1. 读取当前 Jar 内的 MANIFEST.MF
 * 2. 获取业务主类 Start-Class
 * 3. 初始化自定义索引类加载器
 * 4. 启动业务应用
 */
public class JarLauncher {

    private static final String MAIN = "main";
    
	private static final String START_CLASS = "Start-Class";
	
	private static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";

	public static void main(String[] args) throws Exception {
		ClassLoader resourceLoader = JarLauncher.class.getClassLoader();
        // 读取 MANIFEST.MF
        InputStream in = resourceLoader.getResourceAsStream(META_INF_MANIFEST_MF);
        if (in == null) {
            throw new IllegalStateException("未找到 META-INF/MANIFEST.MF");
        }
        Manifest manifest = new Manifest(in);
        Attributes attributes = manifest.getMainAttributes();
        // 获取业务主类
        String startClass = attributes.getValue(START_CLASS);
        if (startClass == null || startClass.isBlank()) {
            throw new IllegalStateException("MANIFEST.MF 中未配置 Start-Class");
        }
        // 父加载器：当前 Jar 的类加载器
        ClassLoader parent = ClassLoader.getPlatformClassLoader();
        // 创建自定义索引类加载器
        JarClassLoader classLoader = new JarClassLoader(parent,resourceLoader);
        Thread.currentThread().setContextClassLoader(classLoader);
        // 启动业务主类
        Class<?> mainClass = classLoader.loadClass(startClass);
        mainClass.getMethod(MAIN, String[].class).invoke(null, (Object) args);
    }

}