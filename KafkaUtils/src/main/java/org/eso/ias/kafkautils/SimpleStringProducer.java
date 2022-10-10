package org.eso.ias.kafkautils;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SimpleStringProducer pushes strings on a Kafka topic.
 *
 *     The producer delegates to a {@link org.apache.kafka.clients.producer.KafkaProducer} and allows
 *     to push data to different topics so that only one instance of this publisher can be used
 *     instead of many {@link SimpleStringProducer} or equivalent.
 *     According to the documentation, the {@link org.apache.kafka.clients.producer.KafkaProducer} is,
 *     in fact thought to be used for this purpose: it is thread safe and generally more efficient than having
 *     multiple producers in the same process.
 *
 *     From a performance point of view, there is a saturation point for th eproducer after which the preformances degrade.
 *     When the saturation point has been reached, the only way to increase performances is to add new producers.
 *
 *     USAGE: normally only one SimpleStringProducer is used for pushing strings in all the topics.
 *     If there are performance problems then instantiate more SimpleStringProducers.
 *
 * 	Kafka properties are fully customizable by calling {@link #setUp(Properties)}:
 * 	defaults values are used for the missing properties.
 *
 *
 *
 * 
 * @author acaproni
 *
 */
public class SimpleStringProducer {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleStringProducer.class);
	
	/**
	 * A list of comma separated servers:port to connect to
	 */
	public final String bootstrapServers;
	
	/**
	 * The kafka producer
	 */
	private KafkaProducer<String, String> producer;
	
	/**
	 * Set if the producer has been closed
	 */
	private final AtomicBoolean closed=new AtomicBoolean(false);

	/**
	 * Set if the producer has been initialized
	 */
	private final AtomicBoolean initialized=new AtomicBoolean(false);
	
	/**
	 * The unique identifier of this producer mapped to kafka client.id property
	 */
	private final String clientID;
	
	/**
	 * The number of strings published in the topic
	 */
	private final AtomicInteger numOfStringsSent = new AtomicInteger(0);

	/**
	 * Constructor 
	 * 
	 * @param servers The list of kafka servers to connect to
	 * @param clientID The unique identifier of this producer (mapped to kafka client.id)
	 */
	public SimpleStringProducer(String servers, String clientID) {
		if (servers==null || servers.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid servers list: expected serve1:port1, server2:port2...");
		}
		this.bootstrapServers=servers.trim();
		if (clientID==null || clientID.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid producer ID");
		}
		this.clientID=clientID.trim();
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

		String newClientId=props.getProperty("client.id");
        if (newClientId!=null) {
            logger.warn("client.id redifined to {}",newClientId);
        }
	}
	
	/**
	 * Build and return the default properties for the consumer
	 * @return
	 */
	private Properties getDefaultProps() {
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("acks", "all");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("client.id",clientID);
		return props;
	}
	
	/**
	 * Initialize the producer with default properties
	 */
	public void setUp() {
		setUp(getDefaultProps());
	}
	
	/**
	 * Initialize the producer with the given properties
	 * <P>
	 * Servers and ID passed in the constructor override those in the passed properties
	 * 
	 * @param props user defined properties
	 */
	public void setUp(Properties props) {
		boolean alreadyInitialized = initialized.getAndSet(true);
		if (!alreadyInitialized) {
			logger.info("Producer [{}] is building the kafka producer", clientID);
			Objects.requireNonNull(props);
			mergeDefaultProps(props);
			producer = new KafkaProducer<>(props);
			logger.info("Kafka producer [{}] built", clientID);
		} else {
			logger.warn("Already initialized");
		}
	}
	
	/**
	 * Closes the producer
	 */
	public void tearDown() {
		boolean alreadyClosed=closed.getAndSet(true);
		if (alreadyClosed) {
			logger.warn("Producer already closed");
			return;
		}
		logger.debug("Closing kafka producer [{}]",clientID);
		producer.close();
		logger.info("Kafka producer [{}] closed",clientID);
	}
	
	/**
	 * Asynchronously pushes a string in a kafka topic.
	 * 
	 * @param value The not <code>null</code> nor empty string to publish in the topic
	 * @param topic The not <code>null</code> nor empty topic to push the string into
	 * @param partition The partition
	 * @param key The key
	 * @throws KafkaUtilsException in case of error sending the value
	 */
	public void push(String value,	String topic, Integer partition,	String key) throws KafkaUtilsException {
		push(value,topic,partition,key,false,0,null);
	}
	
	/**
	 * Synchronously pushes the passed string in the topic
	 * 
	 * @param value The not <code>null</code> nor empty string to publish in the topic
	 * @param topic The not <code>null</code> nor empty topic to push the string into
	 * @param partition The partition
	 * @param key The key
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value
	 */
	public void push(
			String value,
			String topic,
			Integer partition, 
			String key, 
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		push(value,topic,partition,key,true,timeout,unit);
	}
	
	/**
	 * Pushes the passed string in the given partition and key.
	 * <P>
	 * Kafka sending is asynch. i.e. this methods returns
	 * before the actual value is effectively published in the topic unless
	 * the sync parameter is set to <code>true</code> in which case
	 * send waits for the effective sending  
	 * 
	 * @param value The not <code>null</code> nor empty string to publish in the topic
	 * @param topic The not <code>null</code> nor empty topic to push the string into
	 * @param partition The partition
	 * @param key The key
	 * @param sync If true the methods return
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value 
	 */
	protected void push(
			String value,
			String topic,
			Integer partition, 
			String key, 
			boolean sync,
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		if (value==null || value.isEmpty()) {
			throw new KafkaUtilsException("Cannot send empty strings");
		}
		if (topic==null || topic.isEmpty()) {
			throw new KafkaUtilsException("Undefined topic");
		}
		if (sync && (timeout<=0 || unit==null)) {
				throw new IllegalArgumentException("Invalid timeout/unit args. for sync sending");
		}
		
		if (closed.get()) {
			logger.info("Producer [{}] closed: value [{}] discarded",clientID,value);
			return;
		}
		
		ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, partition, key, value);
		if (sync) {
			Future<RecordMetadata> future = producer.send(record);
			try {
				RecordMetadata recordMData = future.get(timeout,unit);
				logger.debug("[{}] sync published [{}] on partition {} of topic {} with an offset of {}",
						clientID,
						value,
						recordMData.partition(),
						recordMData.topic(),
						recordMData.offset());
			} catch (Exception e) {
				throw new KafkaUtilsException("Exception got while sending ["+value+"]",e);
			}
			
		} else {
			producer.send(record, new Callback() {
				@Override
				public void onCompletion(RecordMetadata metadata, Exception exception) {
					if (exception!=null) {
						logger.error("[{}] error async publishing record [{}] on topic {}",
								clientID,
								value,
								topic,
								exception);
					} else {
						logger.debug("[{}] async published [{}] on partition {} of topic {} with an offset of {}",
								clientID,
								value,
								metadata.partition(),
								metadata.topic(),
								metadata.offset());
					}
				}
			});
		}
		numOfStringsSent.incrementAndGet();
	}
	
	/**
	 * Ensures all the records have been delivered to the broker
	 * especially useful while sending records asynchronously and
	 * want be sure they have all beel sent.
	 */
	public void flush() {
		producer.flush();
	}
	
	
	public int getNumOfStringsSent() {
		return numOfStringsSent.get();
	}

}
