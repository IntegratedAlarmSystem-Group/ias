package org.eso.ias.plugin.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The java pojo with the plugin configuration.
 * <P>
 * This object is used by jakson2 parser to read the JSON file.
 * 
 * @author acaproni
 *
 */
public class PluginConfig {
	
	/**
	 * The logger
	 */
	private final Logger logger = LoggerFactory.getLogger(PluginConfig.class);
	
	/**
	 * The ID of the plugin
	 */
	private String id;
	
	/**
	 * The ID of the system monitored by the plugin
	 */
	private String monitoredSystemId;
	
	/**
	 * The name of the server to send monitor point values
	 * and alarms to
	 */
	private String sinkServer;
	
	/**
	 * The port of the server to send monitor point values
	 * and alarms to
	 */
	private int sinkPort;
	
	/**
	 * Additional user defined properties
	 * 
	 * @see Property
	 */
	private Property[] properties;
	
	/**
	 * The values red from the monitored system
	 * 
	 * @see Value
	 */
	private Value[] values;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Setter
	 * 
	 * @param id the id of the plugin
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the sinkServer
	 */
	public String getSinkServer() {
		return sinkServer;
	}

	/**
	 * @param sinkServer the sinkServer to set
	 */
	public void setSinkServer(String sinkServer) {
		this.sinkServer = sinkServer;
	}

	/**
	 * @return the sinkPort
	 */
	public int getSinkPort() {
		return sinkPort;
	}

	/**
	 * @param sinkPort the sinkPort to set
	 */
	public void setSinkPort(int sinkPort) {
		this.sinkPort = sinkPort;
	}

	/**
	 * @return the values
	 */
	public Value[] getValues() {
		return values;
	}

	/**
	 * @param values the values to set
	 */
	public void setValues(Value[] values) {
		this.values = values;
	}
	
	/**
	 * @return The values as a {@link Collection}
	 */
	public Collection<Value> getValuesAsCollection() {
		Collection<Value> ret = new HashSet<>();
		for (Value v: values) {
			ret.add(v);
		}
		return ret;
	}
	
	/**
	 * @return A map of values whose key is
	 *         the ID of the value
	 */
	public Map<String,Value> getMapOfValues() {
		Map<String,Value> ret = new HashMap<>();
		for (Value v: values) {
			ret.put(v.getId(),v);
		}
		return ret;
	}
	
	/**
	 * 
	 * @param id The non empty identifier of the value to get
	 * @return The value with a give id that is
	 *         empty if the array does not contain
	 *         a value with the passed identifier
	 */
	public Optional<Value> getValue(String id) {
		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty identifier");
		}
		return Optional.ofNullable(getMapOfValues().get(id));
	}
	
	/**
	 * Check the correctness of the values contained in this objects:
	 * <UL>
	 * 	<LI>Non empty ID
	 * 	<LI>Non empty sink server name
	 * 	<LI>Valid port
	 * 	<LI>Non empty list of values
	 *  <LI>No duplicated ID between the values
	 *  <LI>Each value is valid
	 * </ul>
	 * 
	 * @return <code>true</code> if the data contained in this object 
	 * 			are correct
	 */
	public boolean isValid() {
		if (id==null || id.isEmpty()) {
			logger.error("Invalid null or empty plugin ID");
			return false;
		}
		if (monitoredSystemId==null || monitoredSystemId.isEmpty()) {
			logger.error("Invalid null or empty monitored system ID");
			return false;
		}
		if (sinkServer==null || sinkServer.isEmpty()) {
			logger.error("Invalid null or empty sink server name");
			return false;
		}
		if (sinkPort<=0) {
			logger.error("Invalid sink server port number {}",sinkPort);
			return false;
		}
		// There must be at least one value!
		if (values==null || values.length<=0) {
			logger.error("No values found");
			return false;
		}
		// Ensure that all the IDs of the values differ
		if (getMapOfValues().keySet().size()!=values.length) {
			logger.error("Some values share the same ID");
			return false;
		}
		// Finally, check the validity of all the values
		long invalidValues=getValuesAsCollection().stream().filter(v -> !v.isValid()).count();
		if (invalidValues!=0) {
			logger.error("Found {} invalid values",invalidValues);
			return false;
		}
		// Check if a property with the same key appears more then once
		long invalidProps=0;
		boolean duplicatedKeys=false;
		if (properties!=null && properties.length>0) {
			for (int t=0; t<properties.length; t++) {
				if (!properties[t].isValid()) {
					invalidProps++;
				}
				for (int j=t+1; j<properties.length; j++) {
					if (properties[t].getKey().equals(properties[j].getKey())) {
						duplicatedKeys=true;
						logger.error("Invalid properties: key {} is defined more then once",properties[t].getKey());
					}
				}
			}
		}
		if (invalidProps>0 || duplicatedKeys) {
			return false;
		}
		// Everything ok
		logger.debug("Plugin {} configuration is valid",id);
		return true;
		
	}
	
	/**
	 * 
	 * @param key The non empty key of the property to get
	 * @return The value of the property with the given key
	 *         or empty if a property with the given key does not exist
	 */
	public Optional<Property> getProperty(String key) {
		if (key==null || key.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty identifier");
		}
		if (properties==null) {
			return Optional.empty();
		}
		List<Property> props = Arrays.asList(properties);
		return Optional.ofNullable(props.stream().filter( prop -> prop.getKey().equals(key)).findAny().orElse(null));
	}

	/**
	 * @return the array of properties
	 */
	public Property[] getProperties() {
		return properties;
	}
	
	/**
	 * Flushes and return the array of {@link Property} in a {@link Properties} object.
	 * <P>
	 * If the a property with the same key appears more the once, it is discarded
	 * and a message logged. 
	 * 
	 * @return The properties as {@link Properties}
	 */
	public Properties getProps() {
		Properties props = new Properties();
		if (properties!=null) {
			for (Property prop: properties) {
				props.setProperty(prop.getKey(), prop.getValue());
			}
		}
		return props;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Property[] properties) {
		this.properties = properties;
	}

	/**
	 * Getter
	 * 
	 * @return The ID of the system monitored by the plugin
	 */
	public String getMonitoredSystemId() {
		return monitoredSystemId;
	}

	/**
	 * Setter
	 * 
	 * @param monitoredSystemId: The ID of the system monitored by the plugin
	 */
	public void setMonitoredSystemId(String monitoredSystemId) {
		this.monitoredSystemId = monitoredSystemId;
	}
}
