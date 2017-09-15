package org.eso.ias.converter;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * The interface to stream the strings received 
 * from the plugin (i.e. the stringified verion of a 
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
	private Function<String, String> mapper;
	
	/**
	 * The ID of the converter.
	 * <P>
	 * The ID of the converter uniquely identify each running 
	 * converter
	 * 
	 * @see #setKafkaProps()
	 */
	private String converterID;
	
	/**
	 * Additional properties whose content
	 * depends on the transport system defined 
	 * in the extenders of this class)
	 * 
	 */
	private Properties props;
	
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
	 */
	public ConverterStream() {}
	
	/**
	 * Inizialize the stream
	 * <P>
	 * After some intialization, it delegates to 
	 * @param converterID The ID of the converter
	 * @param mapper The mapper to translate values id IAS data structs
	 * @param props User defined properties
	 * @throws ConverterStreamException in case of error initializing
	 */
	public void initialize(
			String converterID, 
			Function<String, String> mapper, 
			Properties props) throws ConverterStreamException {
		Objects.requireNonNull(converterID);
		this.converterID = converterID.trim();
		Objects.requireNonNull(mapper);
		this.mapper=mapper;
		Objects.requireNonNull(props);
		this.props=props;
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
			throw new IllegalStateException("Stream not initialized");
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

	/**
	 * @return the mapper
	 */
	public Function<String, String> getMapper() {
		return mapper;
	}

	/**
	 * @return the converterID
	 */
	public String getConverterID() {
		return converterID;
	}

	/**
	 * @return the props
	 */
	public Properties getProps() {
		return props;
	}

}
