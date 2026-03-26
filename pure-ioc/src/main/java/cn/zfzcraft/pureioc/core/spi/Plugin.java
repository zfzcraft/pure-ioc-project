package cn.zfzcraft.pureioc.core.spi;
import java.util.Set;

import cn.zfzcraft.pureioc.core.extension.ExtensionPoint;

/**
 * must be no args constructor
 */
public interface Plugin  extends ExtensionPoint{
	
  void   registerBeanClasses(Set<Class<?>> pluginClasses);
    
}