package org.eso.ias.kafkautils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
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
 * <P><EM>Life cycle</em>: after {@link #setUp()} oe {@link #setUp(Properties)}
 *                         must be called to initialize the object; 
 *                         {@link #tearDown()} must be called when finished using the object;
 *                         {@link #startGettingEvents(StartPosition)} must be called to start
 *                         polling events from the kafka topic
 *
 * {@link #startGettingEvents(StartPosition)} returns when the consumer has been assigned to
 * at least one partition. There are situations when the partitions assigned to the consumer
 * can be revoked and reassigned like for example when another consumer subscribe or disconnect
 * as the assignment of consumers to partitions is left to kafka in this version.
 * 
 * @author acaproni
 *
 */
public class SimpleStringConsumer implements Runnable {
	
	/**
	 * The start position when connecting to a kafka topic
	 * for reading strings
	 * 
	 * @author acaproni
	 */
	public enum StartPosition {
		// The default position, usually set in kafka configurations
		DEFAULT,
		// Get events from the beginning of the partition
		BEGINNING,
		// Get events from the end of the partition
		END
	}
	
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
	 * The name of the topic to get events from
	 */
	private final String topicName;
	
	/**
	 * The ID of the consumer
	 * 
	 * Needed for writing logs as it can happen to have more then
	 * one consumer logging messages 
	 */
	private final String consumerID;
	
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
	private static final int POLLING_TIMEOUT = 60000;
	
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
	 * The boolean set to <code>true</code> when the
	 * consumer has been initialized
	 */
	private volatile AtomicBoolean isInitialized=new AtomicBoolean(false);
	
	/**
	 * The thread getting data from the topic
	 */
	private final AtomicReference<Thread> thread = new AtomicReference<>(null);
	
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
	 * The position to start reading from
	 */
	private StartPosition startReadingPos = StartPosition.DEFAULT; 
	
	/**
	 * Max time to wait for the assignement of partitions before polling
	 * (in minutes)
	 */
	private static final int WAIT_FOR_PARTITIONS_TIMEOUT = 3;
	
	/**
	 * The latch to wait until the consumer has been initialized and
	 * is effectively polling for events
	 */
	private final CountDownLatch polling = new CountDownLatch(1);
	
	/**
	 * Constructor 
	 * 
	 * @param servers The kafka servers to connect to
	 * @param topicName The name of the topic to get events from
	 * @param consumerID the ID of the conumer
	 * @param listener The listener of events published in the topic
	 */
	public SimpleStringConsumer(String servers, String topicName, String consumerID, KafkaConsumerListener listener) {
		Objects.requireNonNull(servers);
		this.kafkaServers = servers;
		Objects.requireNonNull(topicName);
		if (topicName.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty topic name");
		}
		this.topicName=topicName.trim();
		Objects.requireNonNull(consumerID);
		if (consumerID.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty consumer ID");
		}
		this.consumerID=consumerID.trim();
		Objects.requireNonNull(listener);
		this.listener=listener;
		logger.info("SimpleKafkaConsumer [{}] will get events from {} topic connected to kafka broker@{}",
				consumerID,
				this.topicName,
				this.kafkaServers);
	}
	
	/**
	 * Initializes the consumer with the passed kafka properties.
	 * <P> 
	 * The defaults are used if not found in the parameter
	 * 
	 * @param userPros The user defined kafka properties
	 */
	public synchronized void setUp(Properties userPros) {
		Objects.requireNonNull(userPros);
		if (isInitialized.get()) {
			throw new IllegalStateException("Already initialized");
		}
		mergeDefaultProps(userPros);
		consumer = new KafkaConsumer<>(userPros);

		shutDownThread = new Thread() {
			@Override
			public void run() {
				tearDown();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutDownThread);
		
		isInitialized.set(true);
		logger.info("Kafka consumer [{}] initialized",consumerID);
	}

	/**
	 * Start polling events from the kafka channel.
	 * <P>
	 * This method starts the thread that polls the kafka topic 
	 * and returns after the consumer has been assigned to at least 
	 * one partition. 
	 * 
	 * @param startReadingFrom Starting position in the kafka partition
	 * @throws KafkaUtilsException in case of timeout subscribing to the kafkatopic
	 */
	public synchronized void startGettingEvents(StartPosition startReadingFrom) 
	throws KafkaUtilsException {
		Objects.requireNonNull(startReadingFrom);
		if (!isInitialized.get()) {
			throw new IllegalStateException("Not initialized");
		}
		if (thread.get()!=null) {
			logger.error("Consumer [{}] cannot start receiving: already receiving events!",consumerID);
			return;
		}
		startReadingPos = startReadingFrom;
		Thread getterThread = new Thread(this, SimpleStringConsumer.class.getName() + "-Thread");
		getterThread.setDaemon(true);
		thread.set(getterThread);
		getterThread.start();
		
		try {
			if (!polling.await(WAIT_FOR_PARTITIONS_TIMEOUT, TimeUnit.MINUTES)) {
				throw new KafkaUtilsException("Timed out while waiting for assignemn to kafka partitions");
			}
		} catch (InterruptedException e) {
			logger.warn("Consumer [{}] Interrupted",consumerID);
			Thread.currentThread().interrupt();
			throw new KafkaUtilsException(consumerID+" interrupted while waiting for assignemn to kafka partitions", e);
		}
	}
	
	/**
	 * Stop getting events from the kafka topic.
	 * <P>
	 * The user cannot stop the getting of the events because this is 
	 * part of the shutdown.
	 */
	private synchronized void stopGettingEvents() {
		if (thread.get()==null) {
			logger.error("[{}] cannot stop receiving events as I am not receiving events!",consumerID);
			return;
		}		
		consumer.wakeup();
		try {
			thread.get().join(60000);
			if (thread.get().isAlive()) {
				logger.error("The thread of [{}] to get events did not exit",consumerID);
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		thread.set(null);
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
		props.put("group.id", KafkaHelper.DEFAULT_CONSUMER_GROUP);
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("auto.offset.reset", "latest");
		return props;
	}
	
	/**
	 * Initializes the consumer with default kafka properties
	 */
	public void setUp() {
		logger.info("Setting up the kafka consumer [{}]",consumerID);
		
		setUp(getDefaultProps());
	}
	
	/**
	 * Close and cleanup the consumer
	 */
	public synchronized void tearDown() {
		if (isClosed.get()) {
			logger.debug("Consumer [{}] already closed",consumerID);
			return;
		}
		logger.info("Closing consumer [{}]...",consumerID);
		isClosed.set(true);
		Runtime.getRuntime().removeShutdownHook(shutDownThread);
		stopGettingEvents();
		logger.info("Consumer [{}] cleaned up",consumerID);
	}

	/**
	 * The thread to get data out of the topic
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		logger.info("Thread of consumer [{}] to get events from the topic started",consumerID);
		
		if (startReadingPos==StartPosition.DEFAULT) {
			consumer.subscribe(Arrays.asList(topicName));
		} else {
			consumer.subscribe(Arrays.asList(topicName), new ConsumerRebalanceListener() {
				
				/**
				 * Returns a string of topics and partitions contained in the
				 * passed collection
				 * 
				 * @param parts The partitions to generate the string from
				 * @return a string of topic:partition
				 */
				private String formatPartitionsStr(Collection<TopicPartition> parts) {
					String partitions = "";
					for (TopicPartition topicPartition: parts ) {
						partitions=partitions+topicPartition.topic()+":"+topicPartition.partition()+" ";
					}
					return partitions;
				}
				
				@Override
				public void onPartitionsRevoked(Collection<TopicPartition> parts) {
					logger.info("Partition(s) of consumer [{}] revoked: {}",consumerID, formatPartitionsStr(parts));
					
				}
				
				@Override
				public void onPartitionsAssigned(Collection<TopicPartition> parts) {
					logger.info("Consumer [{}] assigned to {} partition(s): {}",
							consumerID,
							parts.size(),
							formatPartitionsStr(parts));
					if (startReadingPos==StartPosition.BEGINNING) {
						consumer.seekToBeginning(new ArrayList<>());
					} else {
						consumer.seekToEnd(new ArrayList<>());
					}
					polling.countDown();
				}
			});
		}
		
		logger.debug("Cosumer [{}] : start polling loop",consumerID);
		while (!isClosed.get()) {
			ConsumerRecords<String, String> records;
	         try {
	        	 records = consumer.poll(POLLING_TIMEOUT);
	        	 logger.debug("Consumer [{}] got an event read with {} records", consumerID, records.count());
	        	 processedRecords.incrementAndGet();
	         } catch (WakeupException we) {
	        	 continue;
	         } 
	         try {
	        	 for (ConsumerRecord<String, String> record: records) {
	        		 logger.debug("Consumer [{}]: notifying listener of [{}] value red from partition {} and offset {} of topic {}",
	        				 consumerID,
	        				 record.value(),
	        				 record.partition(),
	        				 record.offset(),
	        				 record.topic());
	        		 processedStrings.incrementAndGet();
	        		 listener.stringEventReceived(record.value());
	        	 }
	         } catch (Throwable t) {
	        	 logger.error("Consumer [{}] got an exception got processing events: records lost!",consumerID,t);
	         }
	         
	     }
		logger.info("Closing the consumer [{}]",consumerID);
		consumer.close();
		logger.info("Thread of [{}] to get events from the topic terminated",consumerID);
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
