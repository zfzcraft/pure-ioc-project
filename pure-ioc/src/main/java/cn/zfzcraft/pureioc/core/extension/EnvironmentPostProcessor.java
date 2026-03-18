package cn.zfzcraft.pureioc.core.extension;

import cn.zfzcraft.pureioc.core.Environment;
import cn.zfzcraft.pureioc.core.ExtensionPoint;
/**
 * must be no args constructor
 */
public interface EnvironmentPostProcessor extends ExtensionPoint {
	
	void postProcess(Environment environment);

}
