package cn.zfzcraft.pureioc.utils;


public class ResourcesNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2683508502763794217L;

	public ResourcesNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourcesNotFoundException(String message) {
		super(message);
	}

}
