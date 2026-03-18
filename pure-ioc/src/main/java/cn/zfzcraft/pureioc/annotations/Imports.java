package cn.zfzcraft.pureioc.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Imports {
	Class<?>[] value();
}
