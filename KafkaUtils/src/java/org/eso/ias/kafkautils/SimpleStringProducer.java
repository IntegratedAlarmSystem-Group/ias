package org.eso.ias.kafkautils;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleStringProducer pushes strings on a Kafka topic.
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
	 * Default list of servers
	 */
	public static final String defaultBootstrapServers="localhost:9092";
	
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
	 * Initialize the producer
	 * <P>
	 * Servers and ID passed in the constructor override those in the passed properties
	 */
	public void setUp(Properties props) {
		Objects.requireNonNull(props);
		props.put("bootstrap.servers", bootstrapServers);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("client.id",clientID);
		producer = new KafkaProducer<>(props);
	}
	
	/**
	 * Closes the producer
	 */
	public void tearDown() {
		closed=true;
		producer.close();
	}
	
	/**
	 * Pushes the passed string in the given partition and key
	 * 
	 * @param value The not <code>null</code> nor empty string to publish in the topic
	 * @param partition The partition
	 * @param key The key
	 * @return The number of strings published so far
	 */
	public int push(String value, Integer partition, String key) {
		Objects.requireNonNull(value);
		if (value.isEmpty()) {
			logger.info("Empty string rejected");
		}
		if (closed) {
			logger.info("Producer close: [{}] not sent",value);
		}
		
		ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, partition, key, value);
		producer.send(record);
		return numOfStringsSent.incrementAndGet();
	}
	
	public int getNumOfStringsSent() {
		return numOfStringsSent.get();
	}

}
