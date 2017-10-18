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
	 * @see Exception
	 */
	public ConfigurationException(String message) {
		super(message);
	}

	/**
	 * Constructor
	 * 
	 * @see Exception
	 */
	public ConfigurationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor
	 * 
	 * @see Exception
	 */
	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor
	 * 
	 * @see Exception
	 */
	public ConfigurationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
