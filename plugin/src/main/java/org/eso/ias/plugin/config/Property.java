package org.eso.ias.plugin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A POJO to pass properties to the plugin in the form &lt;name,value&gt;
 * 
 * @author acaproni
 *
 */
public class Property {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(Property.class);

	/**
	 * The key of the property
	 */
	private String key;
	
	/**
	 * The value of the property
	 */
	private String value;

	/**
	 * Empty constructor
	 */
	public Property() {
	}
	
	public Property(String key, String value) {
		super();
		assert(key!=null && !key.isEmpty());
		assert(value!=null && !value.isEmpty());
		setKey(key);
		setValue(value);
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		assert(key!=null && !key.isEmpty());
		this.key = key;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		assert(value!=null && !value.isEmpty());
		this.value = value;
	}
	
	public boolean isValid() {
		if (key==null || key.isEmpty()) {
			logger.error("Invalid null or empty property key");
			return false;
		}
		if (value==null || value.isEmpty()) {
			logger.error("Invalid null or empty value for key "+key);
			return false;
		}
		return true;
	}

}
