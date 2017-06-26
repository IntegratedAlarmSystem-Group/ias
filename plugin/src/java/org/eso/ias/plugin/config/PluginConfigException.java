/**
 * 
 */
package org.eso.ias.plugin.config;

import org.eso.ias.plugin.PluginException;

/**
 * The exception returned to notify problems with the
 * configuration of a Plugin
 * 
 * @author acaproni
 *
 */
public class PluginConfigException extends PluginException {

	/**
	 * 
	 */
	public PluginConfigException() {
	}

	/**
	 * @param message The message of the exception
	 */
	public PluginConfigException(String message) {
		super(message);
	}

	/**
	 * @param cause the cause of the exception
	 */
	public PluginConfigException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message The message of the exception
	 * @param cause the cause of the exception
	 */
	public PluginConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message The message of the exception
	 * @param cause the cause of the exception
	 * @param enableSuppression whether or not suppression is enabled or disabled
	 * @param writableStackTrace whether or not the stack trace should be writable
	 */
	public PluginConfigException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
