package cn.zfzcraft.pureioc.annotations;
import java.lang.annotation.*;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalOnPropertity {
	
	String key();
	String value();
}