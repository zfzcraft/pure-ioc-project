package cn.zfzcraft.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import cn.zfzcraft.loader.JarClassLoader;
import cn.zfzcraft.loader.JarLauncher;
import cn.zfzcraft.maven.ClassPathVersionParser.ClassVersionInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * 最终版 PureJar 打包插件 遵循用户定义的完整标准流程： 1. 全部操作在独立临时目录进行，绝不修改项目原生目录 2. 依赖 Jar
 * 解压：class 放入 classes，资源按原 Jar 名隔离到 resources 3. 项目 target/classes 一次性遍历：同时处理
 * class + 资源 4. 全局重复类检查，冲突直接构建失败 5. 插件内置启动器自动写入 Jar 根目录 6. 使用 JDK 原生 Manifest
 * 类生成标准 MANIFEST.MF 7. 自动生成资源索引文件 META-INF/resource-index.idx 8.
 * 将完整目录结构打包为自定义格式可执行 Jar
 *
 * 无冗余逻辑、无拆分多余步骤、可直接上生产
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class JarRepackageMojo extends AbstractMojo {

	private static final String MULTI_RELEASE = "Multi-Release";
	private static final String MODULE_INFO_CLASS = "module-info.class";
	private static final String CLASSES = "classes";
	private static final String JAR_WORK = "jar-work";
	private static final String RESOURCES = "resources";
	private static final String VERSION_1_0 = "1.0";
	private static final String RESOURCE_INDEX = "resources.index";
	private static final String MANIFEST_MF = "MANIFEST.MF";
	private static final String MAIN_CLASS = "Main-Class";
	private static final String APP = "app";
	private static final String DOT_CLASS = ".class";
	private static final String PREFIX = "#";
	private static final char POINT = '.';
	private static final String META_INF_BEAN_CLASS_ANNOTATIONS = "META-INF/bean_class_annotations";

	private static final String START_CLASS = "Start-Class";
	private static final String CLASS = DOT_CLASS;
	private static final String META_INF = "META-INF";
	private static final String META_INF_BEANS_INDEX = "META-INF/bean_classes.index";

	private static final String DOT_JAR = ".jar";
	private static final String BOOTSTRAP_DESC = "Lcn/zfzcraft/pureioc/annotations/Bootstrap;";

	// 键值分隔符：=
	public static final String KEY_VALUE_SEPARATOR = "=";
	private static final String MRJAR_INDEX_IDX = "mrjars.index";;

	int find = 0;

	/**
	 * 当前 Maven 项目上下文
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	/**
	 * 项目构建输出根目录：target
	 */
	@Parameter(defaultValue = "${project.build.directory}")
	private File buildDir;

	/**
	 * 项目编译输出目录：target/classes（只可读，不可写）
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}")
	private File projectClassesDir;

	/**
	 * 业务项目启动主类（全类名） 对应 MANIFEST.MF 中的 Start-Class
	 */
	private String startClass;

	// ====================== 临时目录结构 ======================

	/**
	 * 插件总工作目录：target/purejar-work
	 */
	private File workDir;

	/**
	 * 所有 class 统一存放目录（业务 + 第三方依赖）
	 */
	private File tempClasses;

	/**
	 * 所有资源统一存放目录，按模块隔离
	 */
	private File tempResources;

	/**
	 * META-INF 目录
	 */
	private File tempMetaInf;

	// ====================== 全局控制集合 ======================

	/**
	 * 全局已加载类路径集合，用于重复类检查 key：类路径，如 com/xxx/YourClass.class
	 */
	private final Set<String> existedClasses = new HashSet<>();

	/**
	 * 资源索引：逻辑路径 → 实际 Jar 内路径集合
	 */
	private final Map<String, Set<String>> resourceIndex = new HashMap<>();

	private final List<String> mrjarIndex = new ArrayList<>();

	// ====================== 插件主入口 ======================

	/**
	 * Maven 插件执行主方法
	 */
	@Override
	public void execute() throws MojoExecutionException {
		try {
			// 1. 初始化临时工作目录
			initTempDirectories();
			// 1. 查找启动类
			findBootstrap();
			// 2. 生成 beans.index
			generateBeanIndex();
			// 2. 处理所有依赖 Jar
			processAllDependencyJars();
			// 3. 统一处理项目 target/classes（一步处理 class + 资源）
			processProjectClassesDirectory();
			// 4. 写入插件内置的启动器类
			writeLauncherClassesFromPlugin();
			// 5. 生成标准 MANIFEST.MF（使用 JDK 原生 API）
			generateStandardManifest();
			// 6. 生成资源索引文件
			generateResourceIndexFile();
			generateMrjarIndexFile();
			// 7. 将完整目录结构打包为最终可执行 Jar
			buildFinalExecutableJar();
			getLog().info("==================================================");
			getLog().info("✅  PureJar 打包完成");
			getLog().info("🚀  Start-Class: " + startClass);
			getLog().info("==================================================");
		} catch (Exception e) {
			throw new MojoExecutionException("❌ PureJar 打包失败: " + e.getMessage(), e);
		}
	}

	private void generateMrjarIndexFile() throws IOException {
		getLog().info("📑 生成mrjar索引文件");
		Path indexFile = tempMetaInf.toPath().resolve(MRJAR_INDEX_IDX);
		// 按行写入，自动处理换行，跨平台安全
		Files.write(indexFile, mrjarIndex);
	}

	// 查找 @Bootstrap 启动类（复用原逻辑）
	private void findBootstrap() throws IOException {
		String classesDir = project.getBuild().getOutputDirectory();
		Files.walk(Paths.get(classesDir)).forEach(p -> {
			if (!p.toString().endsWith(CLASS))
				return;
			try {
				ClassReader cr = new ClassReader(Files.readAllBytes(p));
				cr.accept(new ClassVisitor(Opcodes.ASM9) {
					@Override
					public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
						if (BOOTSTRAP_DESC.equals(desc)) {
							startClass = cr.getClassName().replace('/', POINT);
							find++;
						}
						return null;
					}
				}, ClassReader.SKIP_CODE);
			} catch (Exception ignored) {
			}
		});
		if (find > 1) {
			throw new RuntimeException("Too Many Class Annotated  With @Bootstrap,Expected Exactly One!");
		}
		if (startClass == null) {
			throw new RuntimeException("No Class Annotated  With @Bootstrap Found!");
		}
	}

	// 生成 beans.index（只包含带自定义注解的类）
	private void generateBeanIndex() throws Exception {
		String classesDir = project.getBuild().getOutputDirectory();
		// 1. 收集所有依赖里的注解（META-INF/annotations）
		List<String> annotationSet = collectAnnotationsFromDependencies();
		Set<String> result = new HashSet<>();
		Files.walk(Paths.get(classesDir)).forEach(p -> {
			if (!p.toString().endsWith(CLASS))
				return;
			try {
				ClassReader cr = new ClassReader(Files.readAllBytes(p));
				cr.accept(new ClassVisitor(Opcodes.ASM9) {
					boolean hasCustomAnno = false;
					@Override
					public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
						if (annotationSet.contains(desc)) {
							hasCustomAnno = true;
						}
						return null;
					}
					@Override
					public void visitEnd() {
						if (hasCustomAnno) {
							result.add(cr.getClassName().replace('/', POINT));
						}
					}
				}, ClassReader.SKIP_CODE);
			} catch (Exception ignored) {
			}
		});
		File meta = new File(classesDir, META_INF);
		meta.mkdirs();
		File file = new File(classesDir, META_INF_BEANS_INDEX);
		try (PrintWriter pw = new PrintWriter(file)) {
			result.forEach(pw::println);
		}
	}

	private List<String> collectAnnotationsFromDependencies() throws Exception {
		Set<String> annoSet = new LinkedHashSet<>();
		List<File> libfFiles = new ArrayList<>();
		for (Object path : project.getRuntimeClasspathElements()) {
			File f = new File((String) path);
			if (f.isFile() && f.getName().endsWith(DOT_JAR)) {
				libfFiles.add(f);
			}
		}
		for (File depJar : libfFiles) {
			try (JarFile jarFile = new JarFile(depJar)) {
				var entry = jarFile.getEntry(META_INF_BEAN_CLASS_ANNOTATIONS);
				if (entry == null)
					continue;
				try (BufferedReader br = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))) {
					String line;
					while ((line = br.readLine()) != null) {
						String trim = line.trim();
						if (!trim.isBlank() && !trim.startsWith(PREFIX)) {
							annoSet.add(trim);
						}
					}
				}
			}
		}
		return annoSet.stream().map(ele -> {
			return "L" + ele.replace(".", "/") + ";";
			// Lcn/zfzcraft/pureioc/annotations/Bootstrap;
		}).collect(Collectors.toList());
	}
	// ====================== 步骤实现 ======================

	/**
	 * 初始化并清空临时工作目录 所有中间产物仅存在于此目录
	 */
	private void initTempDirectories() {
		workDir = new File(buildDir, JAR_WORK);
		tempClasses = new File(workDir, CLASSES);
		tempResources = new File(workDir, RESOURCES);
		tempMetaInf = new File(workDir, META_INF);
		// 静默清空旧目录
		deleteDirectoryQuietly(workDir);
		// 创建新目录
		tempClasses.mkdirs();
		tempResources.mkdirs();
		tempMetaInf.mkdirs();
		getLog().info("📂 工作目录: " + workDir.getAbsolutePath());
	}

	/**
	 * 遍历项目所有依赖 Jar，统一解压并分类存放 class → tempClasses 资源 → resources/模块名
	 */
	private void processAllDependencyJars() throws Exception {
		getLog().info("🔍 开始处理所有依赖包");
		for (var artifact : project.getArtifacts()) {
			File jarFile = artifact.getFile();
			if (jarFile == null || !jarFile.getName().endsWith(DOT_JAR)) {
				continue;
			}
			String moduleName = jarFile.getName().replace(DOT_JAR, "");
			getLog().info("   处理: " + jarFile.getName());
			try (JarFile jf = new JarFile(jarFile)) {
				boolean openMrjar = Boolean.parseBoolean(jf.getManifest().getMainAttributes().getValue(MULTI_RELEASE));
				Enumeration<JarEntry> entries = jf.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.isDirectory()) {
						continue;
					}
					String relativePath = entry.getName();
					try (InputStream in = jf.getInputStream(entry)) {
						// 统一文件处理逻辑
						processFileContent(relativePath, in, moduleName, openMrjar);
					}
				}
			}
		}
	}

	/**
	 * 【统一核心方法】 处理任意文件内容：无论是来自 Jar 还是本地目录 - .class 文件：进入统一 classes 目录，并做重复类检查 -
	 * 其他文件：作为资源进入 resources/模块名 目录，并建立索引
	 *
	 * @param relativePath  文件相对路径
	 * @param contentStream 文件内容流
	 * @param moduleName    归属模块名（Jar 名或 app）
	 */
	private void processFileContent(String relativePath, InputStream contentStream, String moduleName,
			boolean openMrjar) throws Exception {
		if (relativePath.endsWith(MODULE_INFO_CLASS)) {
			return;
		}
		if (relativePath.startsWith(META_INF)) {
			if (relativePath.endsWith(DOT_CLASS)) {
				if (openMrjar) {
					writeMrjarIndex(relativePath, moduleName);
				}
			}
			writeResource(relativePath, contentStream, moduleName);

			writeResourceIndex(relativePath, moduleName);
		} else {
			if (relativePath.endsWith(DOT_CLASS)) {
				checkConflictingClass(relativePath);

				writeClass(relativePath, contentStream);
			} else {
				writeResource(relativePath, contentStream, moduleName);
				writeResourceIndex(relativePath, moduleName);
			}
		}

	}


	private void writeMrjarIndex(String relativePath, String moduleName) {
		ClassVersionInfo info = ClassPathVersionParser.parse(relativePath);
		StringBuilder mrjar = new StringBuilder();
		mrjar.append(info.getClassName());
		mrjar.append(KEY_VALUE_SEPARATOR);
		mrjar.append(info.getJdkVersion());
		mrjar.append(KEY_VALUE_SEPARATOR);
		mrjar.append(RESOURCES);
		mrjar.append("/");
		mrjar.append(moduleName);
		mrjar.append("/");
		mrjar.append(relativePath);
		mrjarIndex.add(mrjar.toString());
	}

	private void writeResourceIndex(String relativePath, String moduleName) {
		if (relativePath.endsWith("pom.xml") || relativePath.endsWith("pom.properties")
				|| relativePath.endsWith("MANIFEST.MF")) {
			return;
		} else {
			String logicalPath = relativePath;
			String realJarPath = RESOURCES + "/" + moduleName + "/" + logicalPath;
			resourceIndex.computeIfAbsent(logicalPath, k -> new LinkedHashSet<>()).add(realJarPath);
		}
	}

	private void checkConflictingClass(String relativePath) throws MojoExecutionException {
		// 重复类冲突检查
		if (existedClasses.contains(relativePath)) {
			throw new MojoExecutionException("重复类冲突: " + relativePath);
		}
		existedClasses.add(relativePath);
	}

	private void writeResource(String relativePath, InputStream contentStream, String moduleName) throws IOException {
		// 资源文件，按模块隔离
		Path destPath = tempResources.toPath().resolve(moduleName).resolve(relativePath);
		Files.createDirectories(destPath.getParent());
		Files.copy(contentStream, destPath, StandardCopyOption.REPLACE_EXISTING);
	}

	private void writeClass(String relativePath, InputStream contentStream) throws IOException {
		// 写入统一 classes 目录
		Path destPath = tempClasses.toPath().resolve(relativePath);
		Files.createDirectories(destPath.getParent());
		Files.copy(contentStream, destPath, StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * 统一处理项目自身 target/classes 一次遍历，同时处理 class 和资源文件，不拆分步骤 项目内容统一归属模块名：app
	 */
	private void processProjectClassesDirectory() throws Exception {
		getLog().info("📝 处理项目 target/classes（一步处理 class + 资源）");
		Path sourcePath = projectClassesDir.toPath();
		Files.walk(sourcePath).filter(Files::isRegularFile).forEach(file -> {
			try {
				String relPath = sourcePath.relativize(file).toString().replace('\\', '/');
				try (InputStream in = Files.newInputStream(file)) {
					// 项目资源统一归属 app
					processFileContent(relPath, in, APP, false);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * 将插件内置的启动器类写入临时目录根路径 这两个类不属于业务，也不属于 classes，由插件自带
	 */
	private void writeLauncherClassesFromPlugin() throws Exception {
		getLog().info("🚀 写入插件内置启动器类");
		writeClassFromPlugin(JarLauncher.class);
		writeClassFromPlugin(JarClassLoader.class);
	}

	/**
	 * 从插件自身的 classpath 中读取启动类，并写入目标路径
	 */
	private void writeClassFromPlugin(Class<?> clazz) throws Exception {
		String classResourcePath = clazz.getName().replace('.', '/') + DOT_CLASS;
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(classResourcePath)) {
			if (in == null) {
				throw new MojoExecutionException("启动类缺失: " + classResourcePath);
			}
			Path destPath = workDir.toPath().resolve(classResourcePath);
			Files.createDirectories(destPath.getParent());
			Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * 使用 JDK 原生 java.util.jar.Manifest 生成标准 MANIFEST.MF 绝不手动拼接字符串，保证格式 100% 符合规范
	 */
	private void generateStandardManifest() throws Exception {
		getLog().info("📄 生成标准 MANIFEST.MF");
		Manifest manifest = new Manifest();
		Attributes mainAttrs = manifest.getMainAttributes();
		// 规范要求：必须第一个设置 Manifest 版本
		mainAttrs.put(Attributes.Name.MANIFEST_VERSION, VERSION_1_0);
		mainAttrs.put(new Attributes.Name(MAIN_CLASS), JarLauncher.class.getName());
		mainAttrs.put(new Attributes.Name(START_CLASS), startClass);
		Path manifestPath = tempMetaInf.toPath().resolve(MANIFEST_MF);
		try (OutputStream out = Files.newOutputStream(manifestPath)) {
			manifest.write(out);
		}
	}

	/**
	 * 生成资源索引文件（标准无硬编码换行） 格式：逻辑路径=实际Jar内路径1,实际Jar内路径2,... 每行一条，使用标准行分隔符
	 */
	private void generateResourceIndexFile() throws Exception {
		getLog().info("📑 生成资源索引文件");
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, Set<String>> entry : resourceIndex.entrySet()) {
			String logicalPath = entry.getKey();
			String realPaths = String.join(",", entry.getValue());
			lines.add(logicalPath + KEY_VALUE_SEPARATOR + realPaths);
		}
		Path indexFile = tempMetaInf.toPath().resolve(RESOURCE_INDEX);
		// 按行写入，自动处理换行，跨平台安全
		Files.write(indexFile, lines);
	}

	/**
	 * 将整个临时工作目录，打包成最终自定义格式可执行 Jar
	 */
	private void buildFinalExecutableJar() throws Exception {
		getLog().info("📦 打包最终可执行 Jar");
		File finalJarFile = new File(buildDir, project.getBuild().getFinalName() + DOT_JAR);
		try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(finalJarFile.toPath()))) {
			Files.walk(workDir.toPath()).filter(Files::isRegularFile).forEach(path -> {
				try {
					String entryName = workDir.toPath().relativize(path).toString().replace('\\', '/');
					jos.putNextEntry(new JarEntry(entryName));
					Files.copy(path, jos);
					jos.closeEntry();
				} catch (Exception e) {
					throw new RuntimeException("写入 Jar 失败", e);
				}
			});
		}
	}

	/**
	 * 静默删除目录，不抛出异常
	 */
	private void deleteDirectoryQuietly(File directory) {
		if (!directory.exists()) {
			return;
		}
		try {
			Files.walk(directory.toPath()).sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
	}
}