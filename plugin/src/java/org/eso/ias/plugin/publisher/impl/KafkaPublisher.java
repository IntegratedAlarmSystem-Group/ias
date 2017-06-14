package org.eso.ias.plugin.publisher.impl;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.test.publisher.SimpleKafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The publisher of monitor point values through Kafka.
 * <P>
 * <code>KafkaPublisher</code> is an unbuffered publisher because 
 * Kafka already does its own buffering and optimizations.
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
	 * Properties to configure kafka producer
	 */
	private final Properties props = new Properties();
	
	/**
	 * The partition in the topic.
	 * 
	 * TODO: define how to pass the partition number for example:
	 * <UL>
	 * 	<LI>In the json configuration file of the plugin
	 * 	<LI>in the property file
	 *  <LI>java property in the command line
	 * 	<LI>generating a unique ID from the name of the plugin
	 * </UL>
	 */
	private final Integer partition=0;
	
	/**
	 * The topic name for kafka publisher (in the actual implementation all the 
	 * plugins publishes on the same topic but each one has its own partition).
	 */
	public static final String topicName="PluginsKTopic";
	
	/**
	 * The kafka producer
	 */
	private Producer<String, String> producer;
	

	public KafkaPublisher(String pluginId, String serverName, int port, ScheduledExecutorService executorSvc) {
		super(pluginId, serverName, port, executorSvc);
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
		/**
		 * The properties for the kafka publisher.
		 * <P>
		 * TODO: get the properties from a file or other configuration and provide
		 */
		props.put("bootstrap.servers", serverName+":"+serverPort);
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		
		producer = new KafkaProducer<>(props);	
		logger.info("Kafka producer initialized");
		
		logsInfo();
	}

	/**
	 * Close the kafka producer
	 */
	@Override
	protected void shutdown() throws PublisherException {
		producer.close();
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
