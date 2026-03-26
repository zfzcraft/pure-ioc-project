package cn.zfzcraft.pureioc.core.exception;


public class BeanCreationFailedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4524534127913264401L;

	public BeanCreationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
