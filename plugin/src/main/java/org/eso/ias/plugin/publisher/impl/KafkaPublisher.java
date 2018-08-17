package org.eso.ias.plugin.publisher.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The publisher of monitor point values through Kafka.
 * <P>
 * <code>KafkaPublisher</code> is an unbuffered publisher because 
 * Kafka already does its own buffering and optimizations.
 * <P>
 * Kafka topic is hardcoded in {@link #defaultTopicName} property
 * 
 * @author acaproni
 *
 */
public class KafkaPublisher extends PublisherBase {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(KafkaPublisher.class);
	
	/**
	 * The name of the property to set the path of the kafka provided configuration file 
	 */
	public static final String KAFKA_CONFIG_FILE_PROP_NAME = "org.eso.ias.plugin.kafka.config";
	
	/**
	 * The default path of the kafka provided configuration file 
	 */
	public static final String KAFKA_CONFIG_FILE_DEFAULT_PATH = "/org/eso/ias/plugin/publisher/impl/KafkaPublisher.properties";
	
	/**
	 * The name of the (required) java property to set the number of the partition
	 */
	public static final String KAFKA_PARTITION_PROP_NAME = "org.eso.ias.plugin.kafka.partition";
	
	/**
	 * The name of the java property to set the kafka topic.
	 * <P>
	 * If not set, {@link #defaultTopicName} is used.
	 */
	public static final String KAFKA_TOPIC_PROP_NAME = "org.eso.ias.plugin.kafka.topic";
	
	/**
	 * The topic name for kafka publisher (in the actual implementation all the 
	 * plugins publish on the same topic but each one has its own partition).
	 */
	public static final String defaultTopicName=KafkaHelper.PLUGINS_TOPIC_NAME;
	
	/**
	 * The topic name red from the passed property if exists, or the default.
	 */
	public static final String topicName = System.getProperty(KAFKA_TOPIC_PROP_NAME)==null?defaultTopicName:System.getProperty(KAFKA_TOPIC_PROP_NAME);
	
	/**
	 * The partition number is mandatory and must be defined by setting the
	 * {@value #KAFKA_PARTITION_PROP_NAME} java property.  
	 * 
	 */
	private Integer partition=null;
	
	/**
	 * The kafka producer
	 */
	private Producer<String, String> producer = null;
	
	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param serverName The name of the server
	 * @param port The port of the server
	 * @param executorSvc The executor service
	 */
	public KafkaPublisher(
			String pluginId,
			String monitoredSystemId,
			String serverName, 
			int port, 
			ScheduledExecutorService executorSvc) {
		super(pluginId, monitoredSystemId, serverName, port, executorSvc);
	}

	/**
	 * Push a monitor point values in the kafka topic and partition.
	 */
	@Override
	protected long publish(MonitorPointData mpData) throws PublisherException {
		String jsonStrToSend = mpData.toJsonString();
		// The partition is explicitly set: the passed key will not used
		// for partitioning in the topic
		ProducerRecord<String, String> record = new ProducerRecord<String, String>(topicName, partition,pluginId,jsonStrToSend);
		Future<RecordMetadata> future = producer.send(record);
		
		return jsonStrToSend.length();
	}

	/**
	 * Initializes the kafka producer
	 */
	@Override
	protected void start() throws PublisherException {
		
		// Is there a user defined properties file?
		String userKafkaPropFilePath = System.getProperty(KAFKA_CONFIG_FILE_PROP_NAME);
		InputStream userInStream = null;
		if (userKafkaPropFilePath!=null){
			try {
				userInStream=new FileInputStream(userKafkaPropFilePath);
			} catch (IOException ioe) {
				throw new PublisherException("Cannot open the user defined file of properties",ioe);
			}
		}
		
		try (InputStream defaultInStream = KafkaPublisher.class.getResourceAsStream(KAFKA_CONFIG_FILE_DEFAULT_PATH)){
			mergeProperties(defaultInStream, userInStream);
		} catch (IOException ioe) {
			throw new PublisherException("Cannot open the default input file of properties",ioe);
		} catch (PublisherException pe) {
			throw new PublisherException("Cannot merge properties",pe);
		} finally {
			if (userInStream!=null) try {
				userInStream.close();
			} catch (IOException ioe) {
				throw new PublisherException("Error closing the user defined file of properties",ioe);
			}
		}
		
		// Force the hardcoded properties
		System.getProperties().put("bootstrap.servers", serverName+":"+serverPort);
		System.getProperties().put("client.id",pluginId);
		System.getProperties().put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		System.getProperties().put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		
		// Is there a partition number in the java properties? 
		partition = Integer.getInteger(KAFKA_PARTITION_PROP_NAME);
		if (partition==null) {
			logger.info("No kafka partition given: will use the ID {} of the plugin to get the partition",pluginId);
		} else if (partition<0) {
			logger.warn("Invalid Kafka partition number {}: will use the ID {} of the plugin as key to get the partition",
					partition,
					pluginId);
			partition=null;
		} else {
			logger.info("Will use kafka partition {}",partition);
		}
		logger.info("Will use kafka topic {}",topicName);
		producer = new KafkaProducer<>(System.getProperties());	
		logger.info("Kafka producer initialized");
		
		logsInfo();
	}

	/**
	 * Close the kafka producer
	 */
	@Override
	protected void shutdown() throws PublisherException {
		if (producer!=null) {
			producer.close();
		}
	}
	
	/**
	 * Issue some logs about kafka topic that can be useful for debugging
	 */
	private void logsInfo() {
		List<PartitionInfo> partitions = producer.partitionsFor(topicName);
		logger.info("Kafka topic {} has {} partitions",topicName,partitions.size());
		partitions.forEach(p -> logger.debug(p.toString()) );
	}

}
