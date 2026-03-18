package cn.zfzcraft.pureioc.core;

public class IocException extends RuntimeException{

	
	private static final long serialVersionUID = -1499145892638043178L;
	
	public static IocException of(Throwable e) {
		return new IocException(e);
	}

	public IocException() {
		super();
	}

	public IocException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public IocException(String message, Throwable cause) {
		super(message, cause);
	}

	public IocException(String message) {
		super(message);
	}

	public IocException(Throwable cause) {
		super(cause);
	}

	public static IocException of(String message) {
		return new IocException(message);
	}

}
