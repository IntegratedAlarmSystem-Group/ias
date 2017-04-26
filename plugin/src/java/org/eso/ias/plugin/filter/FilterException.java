package org.eso.ias.plugin.filter;

/**
 * The exception returned by filtering
 * 
 * @see Exception
 * 
 * @author acaproni
 *
 */
public class FilterException extends Exception {

	public FilterException() {
	}

	public FilterException(String message) {
		super(message);
	}

	public FilterException(Throwable cause) {
		super(cause);
	}

	public FilterException(String message, Throwable cause) {
		super(message, cause);
	}

	public FilterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
