package cn.zfzcraft.pureioc.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import cn.zfzcraft.pureioc.core.exception.ResourcesNotFoundException;


public class ResourceUtils {
	
	public static InputStream load(String path) {
		return ClassLoaderUtils.getClassLoader().getResourceAsStream(path);
	}

	public static boolean exists(String path) {
		return getResource(path)!=null;
	}
	
	public static Enumeration<URL> getResources(String path) {
		Enumeration<URL> urls =null;
		try {
			urls =ClassLoaderUtils. getClassLoader().getResources(path);
		} catch (Exception e) {
			throw new ResourcesNotFoundException("Resources"+path+"Not Found!",e);
		} 
		return urls;
	}

	public static URL getResource(String path) {
		return ClassLoaderUtils. getClassLoader().getResource(path);
	}
	
	
	public static InputStream getResourceAsStream(String path) {
		return ClassLoaderUtils.getClassLoader().getResourceAsStream(path);
	}
}
