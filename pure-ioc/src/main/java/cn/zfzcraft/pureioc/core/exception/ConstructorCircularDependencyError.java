package cn.zfzcraft.pureioc.core.exception;

public class ConstructorCircularDependencyError extends Error {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8352871842725328184L;

	public ConstructorCircularDependencyError(String message) {
		super(message);
	}

}
