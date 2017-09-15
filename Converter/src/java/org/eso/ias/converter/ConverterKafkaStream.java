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
 * from the plugins and sends JSON strings to the core of the IAS
 * 
 * TODO: pass the kafka serves and ports
 * 
 * @author acaproni
 */
public class ConverterKafkaStream {
	
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
	private static final String DEFAULTPLUGINSINPUTKTOPICNAME = "PluginsKTopic";
	
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
	 * Kafka stream builder
	 */
	private final KStreamBuilder builder = new KStreamBuilder();
	
	/**
	 * The kafka streams
	 */
	private final KafkaStreams streams;
	
	/**
	 * The ID of the converter.
	 * <P>
	 * The ID of the converter identify each running converter
	 * and is used by Kafka
	 * 
	 * @see #setKafkaProps()
	 */
	private final String converterID;
	
	/**
	 * The default name for the topic to send values to the core
	 */
	private static final String DEFAULTCOREKTOPICNAME= "IasCoreKTopic";
	
	/**
	 * Constructor
	 * 
	 * @param converterID The ID of the converter
	 * @param mapper The function to map the received string
	 *               in the string to send to the core
	 */
	public ConverterKafkaStream(String converterID, Function<String, String> mapper) {
		Objects.requireNonNull(mapper);
		Objects.requireNonNull(converterID);
		this.converterID = converterID.trim();
		
		
		String inputTopicName=System.getProperty(PLUGIN_TOPIC_NAME_PROP_NAME);
		pluginsInputKTopicName=(inputTopicName==null)?DEFAULTPLUGINSINPUTKTOPICNAME:inputTopicName;
		String outputTopicName=System.getProperty(PLUGIN_TOPIC_NAME_PROP_NAME);
		iasCoreOutputKTopicName=(outputTopicName==null)?DEFAULTCOREKTOPICNAME:outputTopicName;
		
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
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return props;
	}

}
