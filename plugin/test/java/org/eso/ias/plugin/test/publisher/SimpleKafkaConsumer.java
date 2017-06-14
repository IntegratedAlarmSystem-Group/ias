package org.eso.ias.plugin.test.publisher;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Kafka consumer to get strings from a kafka topic.
 * 
 * @author acaproni
 *
 */
public class SimpleKafkaConsumer implements Runnable {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleKafkaConsumer.class);

	/**
	 * The name of the topic to get events from
	 */
	private final String topicName;
	
	/**
	 * The server name where the kafka broker runs 
	 */
	private final String serverName;
	
	/**
	 * The port used by the kafka broker 
	 */
	private final int serverPort;
	
	/**
	 * The time, in milliseconds, spent waiting in poll if data is not available in the buffer
	 */
	private static final int pollingTimeout = 500;
	
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
	 * Constructor 
	 * 
	 * @param topicName The name of the topic to get events from
	 * @param serverName The server name where the kafka broker runs
	 * @param serverPort The port used by the kafka broker
	 */
	public SimpleKafkaConsumer(String topicName, String serverName, int serverPort) {
		super();
		this.topicName = topicName;
		this.serverName = serverName;
		this.serverPort = serverPort;
	}
	
	/**
	 * Initializes the consumer
	 */
	public void setUp() {
		logger.info("Setting up the kafka consumer");
		Properties props = new Properties();
	     props.put("bootstrap.servers", serverName+":"+serverPort);
	     props.put("group.id", "test");
	     props.put("enable.auto.commit", "true");
	     props.put("auto.commit.interval.ms", "1000");
	     props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	     props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
	     consumer = new KafkaConsumer<>(props);
	     consumer.subscribe(Arrays.asList(topicName));
	     logger.info("Kafka consumer subscribed to {}",topicName);
	     thread = new Thread(this,SimpleKafkaConsumer.class.getName()+"-Thread");
	     thread.setDaemon(true);
	     thread.start();
	     logger.info("Kafka consumer initialized");
	}
	
	/**
	 * Close and cleanup the consumer
	 */
	public void tearDown() {
		logger.info("Closing...");
		isClosed.set(true);
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
	         ConsumerRecords<String, String> records = consumer.poll(pollingTimeout);
	         records.forEach( record -> logger.info("offset = {}, key = {}, value = {}", record.offset(), record.key(), record.value()));
	     }
		logger.info("Thread to get events from the topic terminated");
	}

}
