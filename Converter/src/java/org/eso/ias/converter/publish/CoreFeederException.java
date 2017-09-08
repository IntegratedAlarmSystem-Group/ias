package org.eso.ias.converter.publish;

/**
 * The exception returned by the {@link CoreFeeder}
 * 
 * @author acaproni
 *
 */
public class CoreFeederException extends Exception {

	public CoreFeederException() {
	}

	public CoreFeederException(String message) {
		super(message);
	}

	public CoreFeederException(Throwable cause) {
		super(cause);
	}

	public CoreFeederException(String message, Throwable cause) {
		super(message, cause);
	}

	public CoreFeederException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
