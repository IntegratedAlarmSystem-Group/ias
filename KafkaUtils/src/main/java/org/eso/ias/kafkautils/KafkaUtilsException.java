package org.eso.ias.kafkautils;

/**
 * Exception retunrd by classes in KafkaUtils module
 * @author acaproni
 *
 */
public class KafkaUtilsException extends Exception {

	/**
	 * Constructor
	 */
	public KafkaUtilsException() {}

	public KafkaUtilsException(String message) {
		super(message);
	}

	public KafkaUtilsException(Throwable cause) {
		super(cause);
	}

	public KafkaUtilsException(String message, Throwable cause) {
		super(message, cause);
	}

	public KafkaUtilsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
