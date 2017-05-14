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
	 * @param message
	 */
	public PluginConfigException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public PluginConfigException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public PluginConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public PluginConfigException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
