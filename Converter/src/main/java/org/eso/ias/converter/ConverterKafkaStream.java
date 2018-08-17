package org.eso.ias.converter;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.StreamsBuilder;
import org.eso.ias.kafkautils.KafkaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The kafka stream used by the converter to get JSON strings
 * from the plugins and sends JSON strings to the core of the IAS.
 * <P>
 * The list of kafka servers to connect to and the names of
 * the input and output topics can be passed by means of
 * java properties ({@link ConverterKafkaStream(String, Properties)})
 * or programmatically.
 *
 *
 * @author acaproni
 */
public class ConverterKafkaStream extends ConverterStream {

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(ConverterKafkaStream.class);

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
	 * The name of the topic to send values to the core of the IAS
	 */
	private final String iasCoreOutputKTopicName;

	/**
	 * The name of the java property to get the name of the
	 * topic where plugins push values
	 */
	public static final String IASCORE_TOPIC_NAME_PROP_NAME = "org.eso.ias.converter.kafka.outputstream";

	/**
	 * The name of the property to pass the kafka servers to connect to
	 */
	public static final String KAFKA_SERVERS_PROP_NAME = "org.eso.ias.converter.kafka.servers";

    /**
     * The name of the property to customize the group id of the consumer
     */
    public static final String KAFKA_CONSUMER_GROUP_ID_PROP_NAME = "org.eso.ias.converter.kafka.consumer.groupid";

    /**
     * The group.id used by the consumer of the KafkaStream
     */
    private final String streamConsumerGroupId;

    /**
     * The default groupid of the Kafka consumer
     */
    public static final String DEFAULT_CONSUMER_GROUPID = "ConvertersGroup";

	/**
	 * The list of kafka servers to connect to
	 */
	private final String kafkaServers;

	/**
	 * Kafka stream builder
	 */
	private final StreamsBuilder builder = new StreamsBuilder();

	/**
	 * The kafka streams
	 */
	private KafkaStreams streams;

	/**
	 * The constructor takes the names of the topics
	 * for the streaming from the passed properties or falls
	 * back to the defaults.
	 * 
	 * The constructor takes the string to connect to kafka brokers 
	 * from the parameters of from the passed properties.
	 * The property overrides the value passed in <code>kafkaBrokers</code>.
	 * If the parameter is empty and no property is defined, it falls to default
	 * kafka broker URL defined in {@link KafkaHelper#DEFAULT_BOOTSTRAP_BROKERS}. 
	 *
	 * @param converterID The ID of the converter
	 * @param kafkaBrokers The URL to connect to kafka broker(s) read from the CDB
	 * @param props the properties to get kafka serves and topic anmes
	 */
	public ConverterKafkaStream(
			String converterID,
			Optional<String> kafkaBrokers,
			Properties props) {
		super(converterID);
		Objects.requireNonNull(props);
		Objects.requireNonNull(kafkaBrokers);
		
		Optional<String> brokersFromProperties = Optional.ofNullable(props.getProperty(KAFKA_SERVERS_PROP_NAME));
		if (brokersFromProperties.isPresent()) {
			kafkaServers = brokersFromProperties.get();
		} else if (kafkaBrokers.isPresent()) {
			kafkaServers = kafkaBrokers.get();
		} else {
			kafkaServers = KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS;
		}
		
		pluginsInputKTopicName=props.getProperty(PLUGIN_TOPIC_NAME_PROP_NAME, KafkaHelper.PLUGINS_TOPIC_NAME);
		Objects.requireNonNull(pluginsInputKTopicName);
		iasCoreOutputKTopicName=props.getProperty(IASCORE_TOPIC_NAME_PROP_NAME, KafkaHelper.IASIOs_TOPIC_NAME);
		Objects.requireNonNull(iasCoreOutputKTopicName);
		streamConsumerGroupId=props.getProperty(KAFKA_CONSUMER_GROUP_ID_PROP_NAME,DEFAULT_CONSUMER_GROUPID);

	}

	/**
	 * Constructor
	 *
	 * @param converterID The ID of the converter
	 * @param servers The kafka servers to connect to
	 * @param pluginTopicName The name of the topic to get monitor points from plugins
	 * @param iasCoreTopicName The name of the topic to send values to the core of the IAS
	 */
	public ConverterKafkaStream(
			String converterID,
			String servers,
			String pluginTopicName,
			String iasCoreTopicName) {
		super(converterID);

		Objects.requireNonNull(servers);
		kafkaServers = servers;
		Objects.requireNonNull(pluginTopicName);
		pluginsInputKTopicName = pluginTopicName;
		Objects.requireNonNull(iasCoreTopicName);
		iasCoreOutputKTopicName = iasCoreTopicName;
		streamConsumerGroupId=System.getProperties().getProperty(KAFKA_CONSUMER_GROUP_ID_PROP_NAME,DEFAULT_CONSUMER_GROUPID);
	}

	/**
	 * Initialize the stream
	 *
	 */
	public void init() {
		logger.debug("Initializing...");
		Properties props = setKafkaProps();
		KStream<String, String> source = builder.stream(pluginsInputKTopicName);
        source.mapValues(jString -> mapper.apply(jString)).filter((key,value) -> value!=null && !value.isEmpty()).to(iasCoreOutputKTopicName);
        for (String k: props.stringPropertyNames()) {
            System.out.println("===> prop["+k+"]="+props.getProperty(k));

        }
        streams = new KafkaStreams(builder.build(), props);

        logger.info("Initialized with {} kafka brokers, to send data from {} to {}",
                kafkaServers,
				pluginsInputKTopicName,
				iasCoreOutputKTopicName);
	}



	/**
	 * Set and return the properties for the kafka stream
	 *
	 * @return the properties for the kafka stream
	 */
	private Properties setKafkaProps() {
		Properties props = new Properties();
		// Note that the application.id is used also to assign the group.id of the consumer
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, streamConsumerGroupId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG),"latest");
        // Auto commit cannot be set because the stram uses its own way to commit
        // props.put(StreamsConfig.consumerPrefix(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG),"true");
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
	protected void stopStreaming() throws ConverterStreamException {
		logger.debug("Stopping the streaming...");
		streams.close();
		logger.debug("Streaming terminated");
	}

}
