package cn.zfzcraft.pureioc.core.extension;

import cn.zfzcraft.pureioc.core.Environment;
/**
 * must be no args constructor
 */
public interface EnvironmentPostProcessor extends ExtensionPoint {
	
	void process(Environment environment);
	
	/**
	 * Smaller order executes earlier; larger order executes later.
	 * 
	 * @return
	 */
	int getOrder();

}
