package org.eso.ias.converter;

import java.util.Objects;
import java.util.function.Function;

/**
 * The interface to stream the strings received 
 * from the plugin (i.e. the stringified version of a 
 * monitor point value or alarm) to strings to send 
 * to the core of the IAS (i.e. a stringified representation 
 * of a IAS value) 
 * <P>
 * Classes extending {@link ConverterStream} could need to
 * instantiate a thread (in {@link #start()} to receive data 
 * from the input stream depending on the underlying transport system.
 *  
 * @author acaproni
 */
public abstract class ConverterStream {
	
	/**
	 * The function to map a input string to output string
	 */
	protected final Function<String, String> mapper;
	
	/**
	 * The ID of the converter.
	 * <P>
	 * The ID of the converter uniquely identify each running 
	 * converter
	 * 
	 * @see #setKafkaProps()
	 */
	protected final String converterID;
	
	/**
	 * Flag to signal that the streamer has been initialized
	 */
	private volatile boolean initialized=false;
	
	/**
	 * Flag to signal that the streamer has been initialized
	 */
	private volatile boolean started;
	
	/**
	 * Constructor
	 * 
	 * @param converterID The ID of the converter.
	 * @param mapper The function to map a input string to output string
	 */
	public ConverterStream(String converterID,Function<String, String> mapper) {
		Objects.requireNonNull(converterID);
		if (converterID.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty converter ID");
		}
		this.converterID=converterID.trim();
		Objects.requireNonNull(mapper);
		this.mapper=mapper;
	}
	
	/**
	 * Initialize the stream
	 * <P>
	 * After some initialization, it delegates to {@link #init()}
	 * 
	 * @throws ConverterStreamException in case of error initializing
	 */
	public void initialize() throws ConverterStreamException {
		if (initialized) {
			throw new ConverterStreamException("Already initialized");
		}
		try {
			init();
		} catch (Exception e) {
			throw new ConverterStreamException("Exception initializing",e);
		}
		initialized=true;
	}
	
	/**
	 * User defined initialization
	 * 
	 * @throws ConverterStreamException in case of error initializing
	 */
	protected abstract void init() throws ConverterStreamException;
	
	/**
	 * Start streaming data from the input to the output stream
	 * in a dedicated thread.
	 * <P>
	 * it delegates to the user implemented {@link #startStreaming()}
	 * 
	 * @throws ConverterStreamException in case of error starting
	 */
	public void start() throws ConverterStreamException {
		if (!initialized) {
			throw new ConverterStreamException("Stream not initialized");
		}
		startStreaming();
		started=true;
	}
	
	/**
	 * Stops getting data from the input stream
	 * <P>
	 * it delegates to the user implemented {@link #stopStreaming()}
	 * 
	 * @throws ConverterStreamException in case of error stopping
	 */
	public void stop() throws ConverterStreamException {
		if (!started) {
			return;
		}
		stopStreaming();
	}
	
	/**
	 * Start streaming data from the input to the output stream
	 * in a dedicated thread.
	 * 
	 * @throws ConverterStreamException in case of error starting
	 */
	protected abstract void startStreaming() throws ConverterStreamException;
	
	/**
	 * Stops getting data from the input stream
	 * 
	 * @throws ConverterStreamException in case of error stopping
	 */
	protected abstract void stopStreaming() throws ConverterStreamException;

}
