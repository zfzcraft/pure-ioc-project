package cn.zfzcraft.pureioc.utils;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import cn.zfzcraft.pureioc.core.IocException;
import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Opcodes;

public class AnnotationUtils {

	/**
	 * 判断字节码数组里是否含有指定注解
	 * 
	 * @param inputStream     类字节码
	 * @param beanAnnotationClasses 要判断的注解（如 Component.class, ConditionalOnClass.class）
	 * @return 是否存在
	 */
	public static boolean hasAnnotation(InputStream inputStream, List<Class<? extends Annotation>> beanAnnotationClasses) {
		List<String> annotationsList = new ArrayList<>();
		for (Class<? extends Annotation> beanAnnotationClass : beanAnnotationClasses) {
			String annotationDesc = "L" + beanAnnotationClass.getName().replace('.', '/') + ";";
			annotationsList.add(annotationDesc);
		}
		
		
		final boolean[] found = { false };
		try {
			ClassReader cr = new ClassReader(inputStream);
			cr.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					if (annotationsList.contains(desc)) {
						found[0] = true;
					}
					return super.visitAnnotation(desc, visible);
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

			return found[0];
		} catch (Exception e) {
			throw IocException.of(e);
		}

	}
}