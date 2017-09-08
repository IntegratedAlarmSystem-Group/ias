package org.eso.ias.prototype.input.java;

/**
 * The exception returned serializing/deserializing {@link IASValue}
 * objects to strings.
 * 
 * @author acaproni
 *
 */
public class IasValueSerializerException extends Exception {

	public IasValueSerializerException() {
		// TODO Auto-generated constructor stub
	}

	public IasValueSerializerException(String message) {
		super(message);
		
	}

	public IasValueSerializerException(Throwable cause) {
		super(cause);
		
	}

	public IasValueSerializerException(String message, Throwable cause) {
		super(message, cause);
		
	}

	public IasValueSerializerException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		
	}

}
