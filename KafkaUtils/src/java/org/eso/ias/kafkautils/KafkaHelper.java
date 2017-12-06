package org.eso.ias.kafkautils;

import java.util.Objects;
import java.util.Properties;

/**
 * A helper class providing useful methods and constants for Kafka
 * 
 * @author acaproni
 *
 */
public class KafkaHelper {
	
	/**
	 * Default list of servers
	 */
	public static final String DEFAULT_BOOTSTRAP_BROKERS="localhost:9092";
	
	/**
	 * The name of the property to get the list of kafka servers
	 */
	public static final String BROKERS_PROPNAME="org.eso.ias.kafka.brokers";
	
	/**
	 * The kafka group to which this consumer belongs
	 */
	public static final String DEFAULT_CONSUMER_GROUP = "ias.default.consumer.group";
	
	/**
	 * The name of the property to get the name of the consumer group
	 */
	public static final String CONSUMER_GROUP_PROPNAME="org.eso.ias.kafka.consumer.group";
	
	/**
	 * The topic used by plugins to send monitor point values
	 * and alarms to the converter 
	 */
	public static final String PLUGINS_TOPIC_NAME = "PluginsKTopic";
	
	/**
	 * The name of the kafka topic where converters and DASUs
	 * push the IASIOs
	 */
	public static final String IASIOs_TOPIC_NAME = "BsdbCoreKTopic";
	
	/**
	 * Get the value of a property from the system properties
	 * 
	 * @param propName the name of the property whose value must be get from the system properties
	 * @param defaultValue the default value  if the property is not defined
	 * @return the value of the property
	 */
	public String getValue(String propName, String defaultValue) {
		Objects.requireNonNull(propName);
		if (propName.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty property name");
		}
		Objects.requireNonNull(defaultValue);
		if (defaultValue.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty default value");
		}
		String propValue= System.getProperties().getProperty(propName);
		return (propValue==null)?defaultValue:propValue;
	}
	
	/**
	 * Get the value of a property from the passed properties or, if
	 * not found from the System properties. If the value is not found in both
	 * properties sets then the default is returned.
	 * 
	 * @param userProps the user defined properties
	 * @param propName the name of the property to retrieve
	 * @param defaultValue the default value if the property is not found
	 * @return the value of the property
	 */
	public String getValue(Properties userProps, String propName, String defaultValue) {
		Objects.requireNonNull(userProps);
		Objects.requireNonNull(propName);
		if (propName.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty property name");
		}
		String propValue= userProps.getProperty(propName);
		return (propValue==null)?getValue(propName,defaultValue):propValue;
	}
}
