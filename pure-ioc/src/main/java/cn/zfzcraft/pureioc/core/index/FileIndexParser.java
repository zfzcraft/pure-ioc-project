package cn.zfzcraft.pureioc.core.index;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


public class FileIndexParser {

	public static List<String> parse(InputStream inputStream, Set<Class<? extends Annotation>> annotations) {
		List<String> annotationNames = annotations.stream().map(ele -> ele.getName()).collect(Collectors.toList());
		List<String> classList = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split(":", 2);
				if (parts.length < 2) {
					continue;
				}
				String annotation = parts[0].trim();
				if (annotationNames.contains(annotation)) {
					String className = parts[1].trim();
					classList.add(className);	
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("索引文件解析失败", e);
		}
		return classList;
	}

}