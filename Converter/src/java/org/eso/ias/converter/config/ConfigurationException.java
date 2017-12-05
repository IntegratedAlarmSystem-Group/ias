package org.eso.ias.converter.config;

/**
 * The exception returned by the configuration
 * 
 * @author acaproni
 *
 */
public class ConfigurationException extends Exception {

	/**
	 * Constructor
	 * 
	 * @see Exception
	 */
	public ConfigurationException() {}

	/**
	 * Constructor
	 * 
	 * @param message the message of the exception
	 * @see Exception
	 */
	public ConfigurationException(String message) {
		super(message);
	}

	/**
	 * Constructor
	 * 
	 * @param cause the cause of the exception
	 * @see Exception
	 */
	public ConfigurationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor
	 * 
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 * @see Exception
	 */
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor
	 * 
	 * @param message the message of the exception
	 * @param cause the cause of the exception
	 * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace - whether or not the stack trace should be writable
	 * @see Exception
	 */
	public ConfigurationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
