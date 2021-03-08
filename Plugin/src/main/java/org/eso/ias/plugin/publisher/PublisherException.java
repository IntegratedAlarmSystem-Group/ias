package org.eso.ias.plugin.publisher;

import org.eso.ias.plugin.PluginException;

/**
 * The exception returned by the Publisher
 * 
 * @author acaproni
 *
 */
public class PublisherException extends PluginException {

	public PublisherException(String message) {
		super(message);
	}

	public PublisherException(Throwable cause) {
		super(cause);
	}

	public PublisherException(String message, Throwable cause) {
		super(message, cause);
	}

	public PublisherException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
