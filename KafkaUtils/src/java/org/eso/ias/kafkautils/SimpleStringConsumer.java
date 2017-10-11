package org.eso.ias.kafkautils;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * /**
 * Generic Kafka consumer to get strings from a kafka topic.
 * <P>
 * The string are passed to the listener for further processing.
 * <P>
 * Kafka properties are fully customizable by calling {@link #setUp(Properties)}:
 * defaults values are used for the missing properties.
 * 
 * @author acaproni
 *
 */
public class SimpleStringConsumer implements Runnable {
	
	/**
	 * The listener to be notified of strings read 
	 * from the kafka topic.
	 * 
	 * @author acaproni
	 *
	 */
	public interface KafkaConsumerListener {
		
		/**
		 * Process an event (a String) received from the kafka topic
		 * 
		 * @param event The string received in the topic
		 */
		public void stringEventReceived(String event);
	}
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleStringConsumer.class);
	
	/**
	 * The kafka group to which this consumer belongs
	 */
	private static final String kafkaConsumerGroup = "test";

	/**
	 * The name of the topic to get events from
	 */
	private final String topicName;
	
	/**
	 * The thread to cleanly close the consumer
	 */
	private Thread shutDownThread;
	
	/**
	 * The servers of the kafka broker runs 
	 */
	private final String kafkaServers;
	
	/**
	 * The time, in milliseconds, spent waiting in poll if data is not available in the buffer
	 */
	private static final int pollingTimeout = 60000;
	
	/**
	 * The consumer getting events from the kafka topic
	 */
	private KafkaConsumer<String, String> consumer;
	
	/**
	 * The boolean set to <code>true</code> when the
	 * consumer has been closed
	 */
	private volatile AtomicBoolean isClosed=new AtomicBoolean(false);
	
	/**
	 * The thread getting data from the topic
	 */
	private Thread thread;
	
	/**
	 * The listener of events published in the topic
	 */
	private final KafkaConsumerListener listener;
	
	/**
	 * The number of records received while polling
	 */
	private final AtomicLong processedRecords = new AtomicLong(0);
	
	/**
	 * The number of strings received (a record can contain more strings)
	 */
	private final AtomicLong processedStrings = new AtomicLong(0);
	
	/**
	 * Constructor 
	 * 
	 * @param servers The kafka servers to connect to
	 * @param topicName The name of the topic to get events from
	 * @param listener The listener of events published in the topic
	 */
	public SimpleStringConsumer(String servers, String topicName, KafkaConsumerListener listener) {
		Objects.requireNonNull(servers);
		this.kafkaServers = servers;
		Objects.requireNonNull(topicName);
		if (topicName.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty topic name");
		}
		this.topicName=topicName.trim();
		Objects.requireNonNull(listener);
		this.listener=listener;
		logger.info("SimpleKafkaConsumer will get events from {} topic connected to kafka broker@{}",
				this.topicName,
				this.kafkaServers);
	}
	
	/**
	 * Initializes the consumer with the passed kafka properties.
	 * <P> 
	 * The defaults are used if not passed in the parameter
	 * 
	 * @param userPros The user defined kafka properties
	 */
	public void setUp(Properties userPros) {
		Objects.requireNonNull(userPros);
		mergeDefaultProps(userPros);
		consumer = new KafkaConsumer<>(userPros);
		consumer.subscribe(Arrays.asList(topicName));
		logger.info("Kafka consumer subscribed to {}", topicName);
		Set<TopicPartition> topicPartitions = consumer.assignment();
		Map<TopicPartition, Long> offsets = consumer.beginningOffsets(topicPartitions);
		logger.info("Assigned to {} partitions", topicPartitions.size());
		for (TopicPartition tPart : topicPartitions) {
			logger.info("Partition {} with offset {} on topic {}: next topic to read at {}", tPart.partition(),
					offsets.get(tPart), tPart.topic(), consumer.position(tPart));
		}

		shutDownThread = new Thread() {
			@Override
			public void run() {
				tearDown();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutDownThread);

		thread = new Thread(this, SimpleStringConsumer.class.getName() + "-Thread");
		thread.setDaemon(true);
		thread.start();
		logger.info("Kafka consumer initialized");
	}
	
	/**
	 * Merge the default properties into the passed properties.
	 * 
	 * @param props The properties where missing ones are taken from the default
	 */
	private void mergeDefaultProps(Properties props) {
		Objects.requireNonNull(props);
		
		Properties defaultProps = getDefaultProps();
		defaultProps.keySet().forEach( k -> props.putIfAbsent(k, defaultProps.get(k)));
	}
	
	/**
	 * Build and return the default properties for the consumer
	 * @return
	 */
	private Properties getDefaultProps() {
		Properties props = new Properties();
		props.put("bootstrap.servers", kafkaServers);
		props.put("group.id", kafkaConsumerGroup);
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("auto.offset.reset", "earliest");
		return props;
	}
	
	/**
	 * Initializes the consumer with default kafka properties
	 */
	public void setUp() {
		logger.info("Setting up the kafka consumer");
		
		setUp(getDefaultProps());
	}
	
	/**
	 * Close and cleanup the consumer
	 */
	public void tearDown() {
		logger.info("Closing...");
		isClosed.set(true);
		Runtime.getRuntime().removeShutdownHook(shutDownThread);
		consumer.wakeup();
		try {
			thread.join();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		logger.info("Closing the consumer");
		consumer.close();
		logger.info("Consumer cleaned up");
	}

	/**
	 * The thread to get data out of the topic
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logger.info("Thread to get events from the topic started");
		while (!isClosed.get()) {
			ConsumerRecords<String, String> records;
	         try {
	        	 records = consumer.poll(pollingTimeout);
	        	 logger.debug("Item read with {} records", records.count());
	        	 processedRecords.incrementAndGet();
	         } catch (WakeupException we) {
	        	 continue;
	         } 
	         try {
	        	 for (ConsumerRecord<String, String> record: records) {
	        		 logger.debug("Notifying listener of [{}] value red from partition {} and offset {} of topic {}",
	        				 record.value(),
	        				 record.partition(),
	        				 record.offset(),
	        				 record.topic());
	        		 processedStrings.incrementAndGet();
	        		 listener.stringEventReceived(record.value());
	        	 }
	         } catch (Throwable t) {
	        	 logger.error("Exception got processing events: records lost!",t);
	         }
	         
	     }
		logger.info("Thread to get events from the topic terminated");
	}
	
	/**
	 * @return the number of records processed
	 */
	public long getNumOfProcessedRecords() {
		return processedRecords.get();
	}
	
	/**
	 * @return the number of strings processed
	 */
	public long getNumOfProcessedStrings() {
		return processedStrings.get();
	}

}
