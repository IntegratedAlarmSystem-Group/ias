package org.eso.ias.kafkautils;

import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueSerializerException;
import org.eso.ias.types.IasValueStringSerializer;

import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * KafkaIasioProducer writes {@link IASValue} in the kafka topic by delegating to {@link SimpleStringProducer}
 * <P>
 * The producer converts the IASValues to string with the serializer.
 * <P>
 * KafkaIasioProducer delegates the publishing to {@link SimpleStringProducer}.
 * Normally the {@link SimpleStringProducer} is only one in a process unless there are performance issues:
 * in that case it might help to instantiate a new SimpleStringProducer.
 *
 * For methods that do not specify partition and key, the partition is
 * set to the ID of the IASValue and no partition is used.
 *
 * @see SimpleStringProducer
 * @author acaproni
 */
public class KafkaIasiosProducer {
	
	/**
	 * The Kafka producer to publish IASValues in the kafka topic
	 */
	private final SimpleStringProducer stringProducer;
	
	/**
	 * The serializer to convert AISValues to strings
	 * to be published in the topic
	 */
	private final IasValueStringSerializer serializer;

	/**
	 * The topic to push IASIOs into.
	 *
	 * Normally it is {@link KafkaHelper#IASIOs_TOPIC_NAME}
	 */
	private final String topic;

	/**
	 * Build a KafkaIasioProducer.
	 *
	 * Normally the {@link SimpleStringProducer} is only one in a process unless there are performance issues:
	 * in that case it might help to instantiate a new SimpleStringProducer.
	 * 
	 * @param stringProducer The {@link SimpleStringProducer} to push strings in the topic
	 * @param topic The topic to send strings to
	 * @param serializer The serializer to convert IASValues to strings
	 */
	public KafkaIasiosProducer(SimpleStringProducer stringProducer, String topic, IasValueStringSerializer serializer) {
		Objects.requireNonNull(stringProducer,"The SimpleStringProducer can't be null");
		this.stringProducer = stringProducer;
		if (topic==null || topic.trim().isEmpty()) {
			throw new IllegalArgumentException("Invaluid null/empty topic");
		}
		this.topic=topic.trim();
		Objects.requireNonNull(serializer);
		this.serializer=serializer;
	}
	
	/**
	 * Synchronously pushes the passed value in the topic
	 * 
	 * @param value The not <code>null</code> IASValue to publish in the topic
	 * @param partition The partition
	 * @param key The key
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value
	 */
	public void push(
			IASValue<?> value, 
			Integer partition, 
			String key, 
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		Objects.requireNonNull(value);
		String str = null;
		try {
			str=serializer.iasValueToString(value);
		} catch (IasValueSerializerException e) {
			throw new KafkaUtilsException("Error serializing "+value.toString(),e);
		}
		stringProducer.push(str,topic,partition,key,timeout,unit);
	}
	
	/**
	 * Synchronously pushes the passed value in the topic with default key and partition.
	 * 
	 * @param value The not <code>null</code> IASValue to publish in the topic
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value
	 */
	public void push(
			IASValue<?> value, 
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		Objects.requireNonNull(value);
		String str = null;
		try {
			str=serializer.iasValueToString(value);
		} catch (IasValueSerializerException e) {
			throw new KafkaUtilsException("Error serializing "+value.toString(),e);
		}
		stringProducer.push(str,topic,null,value.id,timeout,unit);
	}

	/**
	 * Asynchronously pushes a IASValue in a kafka topic.
	 * 
	 * @param value The not <code>null</code> value to publish in the topic
	 * @param partition The partition
	 * @param key The key
	 * @throws KafkaUtilsException in case of error sending the value
	 */
	public void push(IASValue<?> value,	Integer partition,	String key) throws KafkaUtilsException {
		Objects.requireNonNull(value);
		String str = null;
		try {
			str=serializer.iasValueToString(value);
		} catch (IasValueSerializerException e) {
			throw new KafkaUtilsException("Error serializing "+value.toString(),e);
		}
		stringProducer.push(str,topic,partition,key);
	}
	
	/**
	 * Asynchronously pushes a IASValue in a kafka topic with default partition and key
	 * 
	 * @param value The not <code>null</code> value to publish in the topic
	 * @throws KafkaUtilsException in case of error sending the value
	 */
	public void push(IASValue<?> value) throws KafkaUtilsException {
		Objects.requireNonNull(value);
		String str = null;
		try {
			str=serializer.iasValueToString(value);
		} catch (IasValueSerializerException e) {
			throw new KafkaUtilsException("Error serializing "+value.toString(),e);
		}
		stringProducer.push(str,topic,null,value.id);
	}
	
	/**
	 * Asynchronously pushes a set of IASValues in a kafka topic.
	 * <P>
	 * This method pushes all the values with the same partition/key so it is not very
	 * convenient. You should probably prefer {@link #push(Collection)}
	 * 
	 * @param values The not <code>null</code> nor empty collection of values to publish in the topic
	 * @param partition The partition to use for sending all the values in the coillection
	 * @param key The key  to use for sending all the values in the coillection
	 * @throws KafkaUtilsException in case of error sending the value
	 */
	public void push(Collection<IASValue<?>> values, Integer partition,	String key) throws KafkaUtilsException {
		Objects.requireNonNull(values);
		if (values.isEmpty()) {
			throw new KafkaUtilsException("Cannot publish empty collection of values");
		}
		for (IASValue<?> value: values) { 
			push(value,partition,key); 
		}
	}
	
	/**
	 * Asynchronously pushes a set of IASValues in a kafka topic.
	 * <P>
	 * This method pushes each value with the default partition/key.
	 * 
	 * @param values The not <code>null</code> nor empty collection of values to publish in the topic
	 * @throws KafkaUtilsException in case of error sending the value
	 */
	public void push(Collection<IASValue<?>> values) throws KafkaUtilsException {
		Objects.requireNonNull(values);
		if (values.isEmpty()) {
			throw new KafkaUtilsException("Cannot publish empty collection of values");
		}
		for (IASValue<?> value: values) { 
			push(value); 
		}
	}
	
	/**
	 * Synchronously pushes the passed values in the topic with default key and partition.
	 * <P>
	 * This method synchronously pushes each value using the passed timeout.
	 * 
	 * @param values The not <code>null</code> nor empty collection of values to publish in the topic
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value
	 */
	public void push(
			Collection<IASValue<?>> values, 
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		Objects.requireNonNull(values);
		if (values.isEmpty()) {
			throw new KafkaUtilsException("Cannot publish empty collection of values");
		}
		for (IASValue<?> value: values) {
			push(value,timeout,unit);
		}
	}
	
	/**
	 * Synchronously pushes the passed value in the topic.
	 * <P>
	 * This method synchronously pushes each value using the passed timeout.
	 * <P>
	 * This method pushes all the values with the same partition/key so it is not very
	 * convenient. You should probably prefer {@link #push(Collection,int,TimeUnit)}.
	 * 
	 * @param values The not <code>null</code> nor empty collection of values to publish in the topic
	 * @param partition The partition
	 * @param key The key
	 * @param timeout the time to wait if sync is set
	 * @param unit the unit of the timeout
	 * @throws KafkaUtilsException in case of error or timeout sending the value
	 */
	public void push(
			Collection<IASValue<?>> values, 
			Integer partition, 
			String key, 
			int timeout,
			TimeUnit unit) throws KafkaUtilsException {
		Objects.requireNonNull(values);
		if (values.isEmpty()) {
			throw new KafkaUtilsException("Cannot publish empty collection of values");
		}
		for (IASValue<?> value: values) {
			push(value,partition,key,timeout,unit);
		}
	}
	
	/**
	 * Ensures all the records have been delivered to the broker
	 * especially useful while sending records asynchronously and
	 * want be sure they have all been sent.
	 */
	public void flush() {
		stringProducer.flush();
	}

	/**
	 * Initialize the producer with default properties
	 */
	public void setUp() {
		stringProducer.setUp();
	}

	/**
	 * Initialize the producer with the given properties
	 * <P>
	 * Servers and ID passed in the constructor override those in the passed properties
	 * 
	 * @param props user defined properties
	 */
	public void setUp(Properties props) {
		stringProducer.setUp(props);
	}

	/**
	 * Closes the producer
	 */
	public void tearDown() {
		stringProducer.tearDown();
	}

	/**
	 * 
	 * @return the number of the values sent
	 */
	public int getNumOfValuessSent() {
		return stringProducer.getNumOfStringsSent();
	}

}
