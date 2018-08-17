package de.nicolasgross.wcttt.core;

public class WctttCoreException extends Exception {

	public WctttCoreException() {
		super();
	}

	public WctttCoreException(String message) {
		super(message);
	}

	public WctttCoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public WctttCoreException(Throwable cause) {
		super(cause);
	}

	protected WctttCoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
