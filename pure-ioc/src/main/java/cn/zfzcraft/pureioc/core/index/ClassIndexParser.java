package cn.zfzcraft.pureioc.core.index;

import org.objectweb.asm.ClassReader;

import cn.zfzcraft.pureioc.core.exception.IgnoreException;
import cn.zfzcraft.pureioc.utils.ResourceUtils;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ClassIndexParser {

	public static List<String> scanByPackage(String basePackage, Set<Class<? extends Annotation>> annotations) {
		List<String> annotationNames = annotations.stream().map(ele -> ele.getName()).collect(Collectors.toList());
		List<String> classList = new ArrayList<>();

		String path = basePackage.replace('.', '/');
		Enumeration<URL> resources = ResourceUtils.getResources(path);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			String protocol = url.getProtocol();
			if ("file".equals(protocol)) {
				try {
					scanDirectory(new File(url.toURI()), classList,annotationNames);
				} catch (Exception e) {
					throw new IgnoreException("ignore", e);
				}
			} else if ("jar".equals(protocol)) {
				try {
					JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
					scanJar(jarFile, path, classList,annotationNames);
				} catch (Exception e) {
					throw new IgnoreException("ignore", e);
				}
			}
		}

		return classList;
	}

	// -------------------------------------------------------------------------
	// 扫描目录
	// -------------------------------------------------------------------------
	private static void scanDirectory(File dir, List<String> classList,List<String> annotationNames) {
		if (!dir.exists())
			return;
		File[] files = dir.listFiles();
		if (files == null)
			return;
		for (File file : files) {
			if (file.isDirectory()) {
				scanDirectory(file, classList,annotationNames);
			} else if (file.getName().endsWith(".class")) {
				try (FileInputStream fis = new FileInputStream(file)) {
					parseClass(fis, classList,annotationNames);
				} catch (Exception e) {
					throw new IgnoreException("ignore", e);
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// 扫描Jar包
	// -------------------------------------------------------------------------
	private static void scanJar(JarFile jarFile, String basePath, List<String> classList,List<String> annotationNames) {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if (name.endsWith(".class") && !entry.isDirectory() && (basePath.isEmpty() || name.startsWith(basePath))) {
				try (InputStream is = jarFile.getInputStream(entry)) {
					parseClass(is, classList,annotationNames);
				} catch (Exception e) {
					throw new IgnoreException("ignore", e);
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// 解析class文件
	// -------------------------------------------------------------------------
	private static void parseClass(InputStream in,List<String> classList,List<String> annotationNames) throws IOException {
		ClassReader cr = new ClassReader(in);
		MetaClassVisitor visitor = new MetaClassVisitor();
		cr.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		String className = visitor.getClassName();
		if (!className.contains("$")) {
			List<String> annotationList = visitor.getAnnotations();
			for (String annotation : annotationList) {
				if (annotationNames.contains(annotation)) {
					classList.add(className);
				}
				
			}
		}

	}

}