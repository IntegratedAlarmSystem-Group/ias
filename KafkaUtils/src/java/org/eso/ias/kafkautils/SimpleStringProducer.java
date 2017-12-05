package org.eso.ias.kafkautils;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleStringProducer pushes strings on a Kafka topic.
 * <P>
 * Kafka properties are fully customizable by calling {@link #setUp(Properties)}:
 * defaults values are used for the missing properties.
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
	private final String bootstrapServers;
	
	/**
	 * The topic to send strings to
	 */
	private final String topic;
	
	/**
	 * The kafka producer
	 */
	private KafkaProducer<String, String> producer;
	
	/**
	 * Set if the producer has been closed
	 */
	private volatile boolean closed=false;
	
	/**
	 * The unique identifier of this producer
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
	 * @param topic The topic to send strings to
	 * @param clientID The unique identifier of this producer
	 */
	public SimpleStringProducer(String servers, String topic, String clientID) {
		Objects.requireNonNull(servers);
		if (servers.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid servers list: expected serve1:port1, server2:port2...");
		}
		this.bootstrapServers=servers.trim();
		Objects.requireNonNull(topic);
		if (topic.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty topic name");
		}
		this.topic=topic.trim();
		Objects.requireNonNull(clientID);
		if (clientID.trim().isEmpty()) {
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
		logger.info("Producer [{}] is building the kafka producer",clientID);
		Objects.requireNonNull(props);
		mergeDefaultProps(props);
		producer = new KafkaProducer<>(props);
		logger.info("Kafka producer [{}] built",clientID);
	}
	
	/**
	 * Closes the producer
	 */
	public void tearDown() {
		logger.info("Closing kafka producer [{}]",clientID);
		closed=true;
		producer.close();
		logger.info("Kafka producer [{}] closed",clientID);
	}
	
	/**
	 * Asynchronously pushes a string in a kafka topic.
	 * 
	 * @param value The not <code>null</code> nor empty string to publish in the topic
	 * @param partition The partition
	 * @param key The key
	 * @throws KafkaUtilsException in case of error sending the value
	 */
	public void push(String value,	Integer partition,	String key) throws KafkaUtilsException {
		push(value,partition,key,false,0,null);
	}
	
	/**
	 * Synchronously pushes the passed string in the topic
	 * 
	 * @param value The not <code>null</code> nor empty string to publish in the topic
	 * @param partition The partition
	 * @param key The key
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value
	 */
	public void push(
			String value, 
			Integer partition, 
			String key, 
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		push(value,partition,key,true,timeout,unit);
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
	 * @param partition The partition
	 * @param key The key
	 * @param sync If true the methods return
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value 
	 */
	protected void push(
			String value, 
			Integer partition, 
			String key, 
			boolean sync,
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		Objects.requireNonNull(value);
		if (value.isEmpty()) {
			throw new KafkaUtilsException("Cannot send empty strings");
		}
		if (sync && (timeout<=0 || unit==null)) {
				throw new IllegalArgumentException("Invalid timeout/unit args. for sync sending");
		}
		
		if (closed) {
			logger.info("Producer [{}] close: [{}] not sent",clientID,value);
			return;
		}
		
		ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, partition, key, value);
		Future<RecordMetadata> future = producer.send(record);
		if (sync) {
			try {
				RecordMetadata recordMData = future.get(timeout,unit);
				logger.info("[{}] published [{}] on partition {} of topic {} with an offset of {}",
						clientID,
						value,
						recordMData.partition(),
						recordMData.topic(),
						recordMData.offset());
			} catch (Exception e) {
				throw new KafkaUtilsException("Exception got while sending ["+value+"]",e);
			}
			
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
