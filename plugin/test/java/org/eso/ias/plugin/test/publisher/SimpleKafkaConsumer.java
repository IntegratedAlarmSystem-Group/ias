package org.eso.ias.plugin.test.publisher;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Kafka consumer to get strings from a kafka topic.
 * <P>
 * The string are passed to the listener for further processing.
 * 
 * @author acaproni
 *
 */
public class SimpleKafkaConsumer implements Runnable {
	
	/**
	 * The listener to be notified of strings read 
	 * from the kafka topic.
	 * 
	 * @author acaproni
	 *
	 */
	public interface KafkaConsumerListener {
		
		/**
		 * Consumeran event (a String) received from the kafka topic
		 * 
		 * @param event The string received in the topic
		 */
		public void consumeKafkaEvent(String event);
	}
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleKafkaConsumer.class);
	
	/**
	 * The kafka group to which this consumer belongs
	 */
	private static final String kafkaConsumerGroup = "test";

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
	 * Constructor 
	 * 
	 * @param topicName The name of the topic to get events from
	 * @param serverName The server name where the kafka broker runs
	 * @param serverPort The port used by the kafka broker
	 * @param listener The listener of events published in the topic
	 */
	public SimpleKafkaConsumer(String topicName, String serverName, int serverPort, KafkaConsumerListener listener) {
		super();
		this.topicName = topicName;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.listener=listener;
		assert(this.listener!=null);
		logger.info("SimpleKafkaConsumer will get events from {} topic connected to kafka broker@{}:{}",
				this.topicName,
				this.serverName,
				this.serverPort);
	}
	
	/**
	 * Initializes the consumer
	 */
	public void setUp() {
		logger.info("Setting up the kafka consumer");
		Properties props = new Properties();
	     props.put("bootstrap.servers", serverName+":"+serverPort);
	     props.put("group.id", kafkaConsumerGroup);
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
	         try {
	        	 ConsumerRecords<String, String> records = consumer.poll(pollingTimeout);
	        	 records.forEach( record -> listener.consumeKafkaEvent(record.value()));
	         } catch (WakeupException we) {
	        	 break;
	         }
	         
	     }
		logger.info("Thread to get events from the topic terminated");
	}

}
