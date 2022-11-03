package org.eso.ias.plugin.publisher.impl;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The publisher of monitor point values through Kafka.
 * <P>
 * <code>KafkaPublisher</code> is an unbuffered publisher because 
 * Kafka already does its own buffering and optimizations.
 * <P>
 * Kafka topic is hardcoded in {@link #defaultTopicName} property.
 *
 * It delegates to {@link SimpleStringProducer}
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
	 * The shared kafka producer
	 */
	private final SimpleStringProducer producer;
	
	/**
	 * Constructor
	 * 
	 * @param pluginId The identifier of the plugin
	 * @param monitoredSystemId The identifier of the system monitored by the plugin
	 * @param producer The shared kafka producer
	 * @param executorSvc The executor service
	 */
	public KafkaPublisher(
			String pluginId,
			String monitoredSystemId,
			SimpleStringProducer producer,
			ScheduledExecutorService executorSvc) {
		super(pluginId, monitoredSystemId, executorSvc);
		Objects.requireNonNull(producer,"The kafka shared producer can't be null");
		this.producer=producer;
	}

	/**
	 * Push a monitor point values in the kafka topic and partition.
	 */
	@Override
	protected long publish(MonitorPointData mpData) throws PublisherException {
        synchronized (iso8601dateFormat) {
            mpData.setPublishTime(iso8601dateFormat.format(new Date(System.currentTimeMillis())));
        }

		String jsonStrToSend = mpData.toJsonString();

        try {
        	producer.push(jsonStrToSend,topicName,null,pluginId);
		} catch (Exception e) {
        	throw new PublisherException(e);
		}
		return jsonStrToSend.length();
	}

	/**
	 * Initializes the kafka producer: does nothing because the
	 * kafka producer is shared and the initialization and closed will be done by the owner
	 */
	@Override
	protected void start() throws PublisherException {
	}

	/**
	 * Close the kafka producer: does nothing because the
	 * kafka producer is shared and the initialization and closed will be done by the owner
	 */
	@Override
	protected void shutdown() throws PublisherException {}

}
