package org.eso.ias.plugin.config;

/**
 * A POJO to pass properties to the plugin in the form <name,value>
 * 
 * @author acaproni
 *
 */
public class Property {

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

}
