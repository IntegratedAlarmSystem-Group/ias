package org.eso.ias.converter;

/**
 * The exception returned by the converter stream.
 * 
 * @author acaproni
 */
public class ConverterStreamException extends Exception {

	
	public ConverterStreamException() {
	
	}

	public ConverterStreamException(String message) {
		super(message);
	}

	public ConverterStreamException(Throwable cause) {
		super(cause);
	}

	public ConverterStreamException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConverterStreamException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
