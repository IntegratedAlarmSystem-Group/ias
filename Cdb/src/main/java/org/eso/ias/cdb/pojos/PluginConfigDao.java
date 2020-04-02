package org.eso.ias.cdb.pojos;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;

/**
 * The java pojo with the plugin configuration.
 * <P>
 * This object is used by jakson2 parser to read the JSON file.
 *
 * @author acaproni
 *
 */
@Entity
@Table(name = "PLUGIN")
public class PluginConfigDao {

	/**
	 * The logger
	 */
	@Transient
	private final Logger logger = LoggerFactory.getLogger(PluginConfigDao.class);

	/**
	 * The ID of the plugin
	 */
	@Id
	@Column(name = "plugin_id")
	private String id;

	/**
	 * The ID of the system monitored by the plugin
	 */
	@Basic(optional=false)
	private String monitoredSystemId;



	/**
	 * Additional user defined properties
	 *
	 * @see PropertyDao
	 */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinTable(name= "PROPS_PLUGIN",
			joinColumns = @JoinColumn(name="plugin_id"),
			inverseJoinColumns = @JoinColumn(name = "props_id"))
	private Set<PropertyDao> props = new HashSet<>();

	/**
	 * The values red from the monitored system
	 *
	 * @see ValueDao
	 */
	@Basic(optional=false)
	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(name= "PLUGIN_VALUE",
			joinColumns = @JoinColumn(name="plugin_id"),
			inverseJoinColumns = @JoinColumn(name = "value_id"))
	private Set<ValueDao> values = new HashSet<>();

	/**
	 *	The global default filter and filterOptions
	 */
	@Basic(optional=true)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String defaultFilter=null;

	/**
	 *	The global default filterOptions
	 */
	@Basic(optional=true)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String defaultFilterOptions=null;


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
	 * @return the values
	 */
	public Set<ValueDao> getValues() {

		return values;
	}

	/**
	 * @param values the values to set
	 */
	public void setValues(Set<ValueDao> values) {
		if (values==null) {
			this.values = new HashSet<>();
		} else {
			this.values = values;
		}
	}


	/**
	 * @return The values as a {@link Collection}
	 */
	public Collection<ValueDao> valuesAsCollection() {
		Collection<ValueDao> ret = new HashSet<>();
		for (ValueDao v: values) {
			ret.add(v);
		}
		return ret;
	}

	/**
	 * @return A map of values whose key is
	 *         the ID of the value
	 */
	public Map<String,ValueDao> valuesAsMap() {
		Map<String,ValueDao> ret = new HashMap<>();
		for (ValueDao v: values) {
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
	public Optional<ValueDao> getValue(String id) {
		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty identifier");
		}
		return Optional.ofNullable(valuesAsMap().get(id));
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
	@JsonIgnore
	public boolean isValid() {
		if (id==null || id.isEmpty()) {
			logger.error("Invalid null or empty plugin ID");
			return false;
		}
		if (monitoredSystemId==null || monitoredSystemId.isEmpty()) {
			logger.error("Invalid null or empty monitored system ID of plugin {}",id);
			return false;
		}


		// There must be at least one value!
		if (values==null || values.isEmpty()) {
			logger.error("No values defined for plugin {}",id);
			return false;
		}
		
		// Ensure that all the IDs of the values differ
		if (valuesAsMap().keySet().size()!=values.size()) {
			logger.error("Some of the values of the plugin {} have the same ID",id);
			return false;
		}
		// Finally, check the validity of all the values
		long invalidValues=valuesAsCollection().stream().filter(v -> !v.valid()).count();
		if (invalidValues!=0) {
			logger.error("Found {} invalid values for plugin {}",invalidValues,id);
			return false;
		}

		// Check if all the properties are valid
		long invalidProps=0;
		for (PropertyDao p: props) {
			if (!p.valid()) {
				invalidProps++;
				logger.error("Invalid property {} of plugin {}",p.toString(),id);
			}
		}

		// Check if a property with the same key appears more then once
		boolean duplicatedKeys=props.size()!=getProps().size();
		if (duplicatedKeys) {
			logger.error("Plugin {} properties contain duplicates (names must be unique)",id);
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
	public Optional<PropertyDao> getProperty(String key) {
		if (key==null || key.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty identifier");
		}
		if (props==null || props.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(props.stream().filter( prop -> prop.getName().equals(key)).findAny().orElse(null));
	}

	/**
	 * @return the array of properties
	 */
	public Set<PropertyDao> getProps() {
		return props;
	}

	/**
	 * Flushes and return the array of {@link PropertyDao} in a {@link Properties} object.
	 * <P>
	 * If the a property with the same key appears more the once, it is discarded
	 * and a message logged.
	 *
	 * @return The properties as {@link Properties}
	 */
	@JsonIgnore
	public Properties getProperties() {
		Properties properties = new Properties();
		if (props!=null) {
			for (PropertyDao prop: props) {
				properties.setProperty(prop.getName(), prop.getValue());
			}
		}
		return properties;
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
	/*is


	 *	Add getter and setter for DefaultFilter and defaultFilterOptions
	 */
	/**
	 * @return the DefaultFilter
	 */
	public String getDefaultFilter() {
		return defaultFilter;
	}

	/**
	 * @param defaultFilter the default filter to set
	 */
	public void setDefaultFilter(String defaultFilter) {
		this.defaultFilter = defaultFilter;
	}

	/**
	 * @return the getDefaultFilterOption
	 */
	public String getDefaultFilterOptions() {
		return defaultFilterOptions;
	}

	/**
	 * @param defaultFilterOptions the default filter options to set
	 */
	public void setDefaultFilterOptions(String defaultFilterOptions) {
		this.defaultFilterOptions = defaultFilterOptions;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PluginConfigDao that = (PluginConfigDao) o;
		return id.equals(that.id) &&
				monitoredSystemId.equals(that.monitoredSystemId) &&
				Objects.equals(props, that.props) &&
				Objects.equals(values, that.values) &&
				Objects.equals(defaultFilter, that.defaultFilter) &&
				Objects.equals(defaultFilterOptions, that.defaultFilterOptions);
	}
}

