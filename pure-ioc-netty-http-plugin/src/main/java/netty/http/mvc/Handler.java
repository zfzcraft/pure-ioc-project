package netty.http.mvc;

import java.lang.reflect.Method;


public class Handler {
	
	private Class<?> controllerClass;
	
	private Object controller;
	
	private	Method method;
	
	public Handler(Class<?> controllerClass, Method method) {
		super();
		this.controllerClass = controllerClass;
		this.method = method;
	}
	
	
	public Class<?> getControllerClass() {
		return controllerClass;
	}


	public void setControllerClass(Class<?> controllerClass) {
		this.controllerClass = controllerClass;
	}


	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public Object getController() {
		return controller;
	}
	public void setController(Object controller) {
		this.controller = controller;
	}
	

	

}
