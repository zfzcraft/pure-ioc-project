package cn.zfzcraft.pureioc.core.factory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import cn.zfzcraft.pureioc.annotations.ConfigurationProperties;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.Environment;
import cn.zfzcraft.pureioc.core.exception.BeanCreationFailedException;
import cn.zfzcraft.pureioc.core.extension.BeanFactory;


public class ConfigurationPropertiesBeanFactory implements BeanFactory {

	@Override
	public Object createBean(ApplicationContext applicationContext, AnnotatedElement beanElement) {
		Class<?> beanClass = (Class<?>) beanElement;
		Environment environment = applicationContext.getEnvironment();
		ConfigurationProperties configurationProperties = beanClass.getAnnotation(ConfigurationProperties.class);
		String prefix = configurationProperties.prefix();
		Object beanObject = environment.getProperty(prefix, beanClass);
		try {
			if (beanObject == null) {
				beanObject = createIfNull(null, beanClass);
			} else {
				beanObject = createIfNull(beanObject, beanClass);
			}
		} catch (Exception e) {
			throw new BeanCreationFailedException("Bean Creation Failed,must be on args Constructor", e);
		}
		return beanObject;
	}

	private Object createIfNull(Object config, Class<?> type) throws Exception {
		if (config != null) {
			initNestedObjects(config);
			return config;
		}
		Object instance = type.getDeclaredConstructor().newInstance();
		initNestedObjects(instance);
		return instance;

	}

	private void initNestedObjects(Object obj) throws Exception {
		if (obj == null)
			return;
		Class<?> clazz = obj.getClass();
		if (isSimpleType(clazz))
			return;
		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			Object value = field.get(obj);
			if (value != null) {
				initNestedObjects(value);
				continue;
			}
			Class<?> fieldType = field.getType();
			if (isSimpleType(fieldType) || fieldType.isInterface() || fieldType.isPrimitive() || fieldType.isArray()
					|| fieldType.isEnum()) {
				continue;
			}
			Object nested = fieldType.getDeclaredConstructor().newInstance();
			field.set(obj, nested);
			initNestedObjects(nested);
		}
	}

	private boolean isSimpleType(Class<?> type) {
		return type.isPrimitive() || type.isEnum() || Number.class.isAssignableFrom(type)
				|| CharSequence.class.isAssignableFrom(type) || Boolean.class == type
				|| List.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
	}

}
