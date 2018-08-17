package de.nicolasgross.wcttt.core;

public class WctttCoreFatalException extends RuntimeException {

	public WctttCoreFatalException() {
		super();
	}

	public WctttCoreFatalException(String message) {
		super(message);
	}

	public WctttCoreFatalException(String message, Throwable cause) {
		super(message, cause);
	}

	public WctttCoreFatalException(Throwable cause) {
		super(cause);
	}

	protected WctttCoreFatalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
