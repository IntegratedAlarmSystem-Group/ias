package org.eso.ias.converter;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The kafka stream used by the converter to get JSON strings
 * from the plugins and sends JSON strings to the core of the IAS.
 * <P>
 * The list of kafka servers to connect to and the names of
 * the input and output topics can be passed by means of
 * java properties ({@link #ConverterKafkaStream(Properties)})
 * or programmatically. 
 * 
 * 
 * @author acaproni
 */
public class ConverterKafkaStream extends ConverterStream {
	
	/**
	 * The logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(ConverterKafkaStream.class);
	
	/**
	 * The name of the topic where plugins pushes
	 * monitor point values and alarms
	 */
	private final String pluginsInputKTopicName;
	
	/**
	 * The name of the java property to get the name of the 
	 * topic where plugins push values
	 */
	private static final String PLUGIN_TOPIC_NAME_PROP_NAME = "org.eso.ias.converter.kafka.inputstream";
	
	/**
	 * The default name for the topic where plugins push values
	 */
	public static final String DEFAULTPLUGINSINPUTKTOPICNAME = "PluginsKTopic";
	
	/**
	 * The name of the topic to send values to the core of the IAS
	 */
	private final String iasCoreOutputKTopicName;
	
	/**
	 * The name of the java property to get thename of the 
	 * topic where plugins push values
	 */
	private static final String IASCORE_TOPIC_NAME_PROP_NAME = "org.eso.ias.converter.kafka.outputstream";
	
	/**
	 * The name of the property to pass the kafka servers to connect to
	 */
	private static final String KAFKA_SERVERS_PROP_NAME = "org.eso.ias.converter.kafka.servers";
	
	/**
	 * Default list of kafka servers to connect to
	 */
	private static final String DEFAULTKAFKASERVERS = "localhost:9092";
	
	/**
	 * The list of kafka servers to connect to
	 */
	private final String kafkaServers;
	
	/**
	 * Kafka stream builder
	 */
	private final KStreamBuilder builder = new KStreamBuilder();
	
	/**
	 * The kafka streams
	 */
	private KafkaStreams streams;
	
	/**
	 * The default name for the topic to send values to the core
	 */
	public static final String DEFAULTCOREKTOPICNAME= "IasCoreKTopic";
	
	/**
	 * The empty constructor gets the kafka servers, and the topics
	 * for the streaming from the passed properties or falls
	 * back to the defaults.
	 * 
	 * @param converterID The ID of the converter.
	 * @param mapper The function to map a input string to output string
	 * @parm props the properties to get kafka serves and topic anmes
	 */
	public ConverterKafkaStream(
			String converterID,
			Function<String, String> mapper,
			Properties props) {
		super(converterID,mapper);
		Objects.requireNonNull(props);
		kafkaServers = props.getProperty(KAFKA_SERVERS_PROP_NAME,DEFAULTKAFKASERVERS);
		pluginsInputKTopicName=props.getProperty(PLUGIN_TOPIC_NAME_PROP_NAME, DEFAULTPLUGINSINPUTKTOPICNAME);
		iasCoreOutputKTopicName=props.getProperty(IASCORE_TOPIC_NAME_PROP_NAME, DEFAULTCOREKTOPICNAME);
		
		logger.debug("Will connect to {} to send data from {} to {}",
				kafkaServers,
				pluginsInputKTopicName,
				iasCoreOutputKTopicName);
	}
	
	/**
	 * Constructor
	 * 
	 * @param converterID The ID of the converter
	 * @param mapper The function to map a input string to output string
	 * @param servers The kafka servers to conncet to
	 * @param pluginTopicName The name of the topic to get monitor points from plugins
	 * @param iasCoreTopicName The name of the topic to send values to the core of the IAS
	 */
	public ConverterKafkaStream(
			String converterID,
			Function<String, String> mapper,
			String servers,
			String pluginTopicName, 
			String iasCoreTopicName) {
		super(converterID,mapper);
		Objects.requireNonNull(servers);
		kafkaServers = servers;
		Objects.requireNonNull(pluginTopicName);
		pluginsInputKTopicName = pluginTopicName;
		Objects.requireNonNull(iasCoreTopicName);
		iasCoreOutputKTopicName = iasCoreTopicName;
		logger.debug("Will connect to {} to send data from {} to {}",
				kafkaServers,
				pluginsInputKTopicName,
				iasCoreOutputKTopicName);
	}
	
	/**
	 * Initialize the stream
	 * 
	 */
	public void init() {
		
		KStream<String, String> source = builder.stream(pluginsInputKTopicName);
        source.mapValues(jString -> mapper.apply(jString)).to(iasCoreOutputKTopicName);
        streams = new KafkaStreams(builder, setKafkaProps());
	}
	
	
	
	/**
	 * Set and return the properties for the kafka stream
	 * 
	 * @return the properties for the kafka stream
	 */
	private Properties setKafkaProps() {
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, converterID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return props;
	}

	/**
	 * Start streaming data from the kafka input topic
	 * to the kafka output topic
	 * 
	 * @see org.eso.ias.converter.ConverterStream#start()
	 */
	@Override
	protected void startStreaming() throws ConverterStreamException {
		logger.debug("Starting the streaming...");
		streams.start();
		logger.info("Streaming activated");
	}

	/**
	 * Stop streaming data
	 * 
	 * @see org.eso.ias.converter.ConverterStream#stop()
	 */
	@Override
	public void stopStreaming() throws ConverterStreamException {
		logger.debug("Stopping the streaming...");
		streams.close();
		logger.debug("Streaming terminated");
	}

}
