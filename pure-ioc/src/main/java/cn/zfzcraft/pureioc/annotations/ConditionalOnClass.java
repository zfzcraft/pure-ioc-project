package cn.zfzcraft.pureioc.annotations;
import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalOnClass {
	
	String className();
}