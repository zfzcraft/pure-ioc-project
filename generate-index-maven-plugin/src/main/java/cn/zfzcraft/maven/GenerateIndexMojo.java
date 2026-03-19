package cn.zfzcraft.maven;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
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
@Mojo(name = "generate-index", defaultPhase = LifecyclePhase.PROCESS_CLASSES,threadSafe = true)
public class GenerateIndexMojo extends AbstractMojo {

	private static final String DOT_CLASS = ".class";
	private static final String PREFIX = "#";
	private static final char POINT = '.';
	private static final String META_INF_BEAN_CLASS_ANNOTATIONS = "META-INF/bean_class_annotations";

	private static final String CLASS = DOT_CLASS;
	private static final String META_INF = "META-INF";
	private static final String META_INF_BEANS_INDEX = "META-INF/bean_classes.index";

	private static final String DOT_JAR = ".jar";

	// 键值分隔符：=
	public static final String KEY_VALUE_SEPARATOR = "=";


	int find = 0;

	/**
	 * 当前 Maven 项目上下文
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;



	/**
	 * Maven 插件执行主方法
	 */
	@Override
	public void execute() throws MojoExecutionException {
		
			
			try {
				generateBeanIndex();
			} catch (Exception e) {
				throw new RuntimeException(e);
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
}
