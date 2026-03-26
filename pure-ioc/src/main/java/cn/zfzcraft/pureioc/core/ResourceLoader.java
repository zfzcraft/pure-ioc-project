package cn.zfzcraft.pureioc.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;


public class ResourceLoader {

	public static InputStream load(String path) {
		return getClassLoader().getResourceAsStream(path);
	}

	public static Enumeration<URL> getResources(String path) throws IOException {
		return getClassLoader().getResources(path);
	}

	public static URL getResource(String path) {
		return getClassLoader().getResource(path);
	}

	public static ClassLoader getClassLoader() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = ResourceLoader.class.getClassLoader();
		}
		return classLoader;
	}

	public static InputStream getResourceAsStream(String path) {
		return getClassLoader().getResourceAsStream(path);
	}
}
