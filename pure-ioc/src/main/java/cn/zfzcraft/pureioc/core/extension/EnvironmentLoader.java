package cn.zfzcraft.pureioc.core.extension;

import java.util.Map;

import cn.zfzcraft.pureioc.core.Environment;
import cn.zfzcraft.pureioc.core.ExtensionPoint;
/**
 * must be no args constructor
 */
public interface EnvironmentLoader extends ExtensionPoint{

	/**
	 * 
	 * @param local
	 * @return nested map
	 */
	Map<String, Object> load(Environment local);
}
