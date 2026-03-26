package cn.zfzcraft.pureioc.core.index;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;


//-------------------------------------------------------------------------
// ASM类访问器
// -------------------------------------------------------------------------
public class MetaClassVisitor extends ClassVisitor {

	private String className;
	
	private final List<String> annotations = new ArrayList<>();

	public MetaClassVisitor() {
		super(org.objectweb.asm.Opcodes.ASM9);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] ifaces) {
		this.className = name.replace('/', '.');
	}

	@Override
	public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		String anno = desc.substring(1, desc.length() - 1).replace('/', '.');
		annotations.add(anno);
		return super.visitAnnotation(desc, visible);
	}

	public String getClassName() {
		return className;
	}
	public List<String> getAnnotations() {
		return annotations;
	}
}