package netty.http.mvc;

import java.lang.annotation.Annotation;


import cn.zfzcraft.pureioc.annotations.Extension;
import cn.zfzcraft.pureioc.core.extension.BeanFactoryAnnotationMatcher;
@Extension
public class HttpAnnotationFactoryBeanMatcher implements BeanFactoryAnnotationMatcher{

	
	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return RestController.class;
	}

}
