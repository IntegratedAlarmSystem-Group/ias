package org.eso.ias.plugin.config;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The value of a monitor point or alarm
 * read from the monitored system.
 *
 * @author acaproni
 *
 */
public class Value {

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(Value.class);

	/**
	 * The unique ID of the value
	 */
	private String id;

	/**
	 * The time interval (msec) to send the value to the
	 * server if it does not change
	 */
	private int refreshTime;

	/**
	 * The optional filter to apply to the value
	 */
	private String filter;

	/**
	 * A optional comma separated list of options to pass
	 * to the filter
	 */
	private String filterOptions;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the refreshTime
	 */
	public int getRefreshTime() {
		return refreshTime;
	}

	/**
	 * @param refreshTime the refreshTime to set
	 */
	public void setRefreshTime(int refreshTime) {
		this.refreshTime = refreshTime;
	}

	/**
	 * @return the filter
	 */
	public String getFilter() {
		if(Objects.isNull(filter)){
			return getDefaultFilter();
		}else{
			return filter;
		}

	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(String filter) {
		if(Objects.isNull(filter)){
			this.filter = getDefaultFilter();
		}else{
			this.filter=filter;		
		}
	}

	/**
	 * @return the filterOptions
	 */
	public String getFilterOptions() {
		if(Objects.isNull(filterOptions)){
			return getDefaultFilterOptions();
		}else{
			return filterOptions;
		}
	}

	/**
	 * @param filterOptions the filterOptions to set
	 */
	public void setFilterOptions(String filterOptions) {
		if(Objects.isNull(filterOptions)){
			this.filterOptions = getDefaultFilterOptions();
		}else{
			this.filterOptions=filterOptions;		
		}
	}

	/*
	 * Set and get defaultFilter and defaultFilterOptions
	 *
	 */


	/**
	 * @return the global Filter
	 */
	public String getDefaultFilter() {
		return PluginConfig.defaultFilter;
	}

	/**
	 * @param defaultFilter the default filter to set
	 */
	public void setDefaultFilter(String defaultFilter) {
		PluginConfig.defaultFilter = defaultFilter;
	}

	/**
	 * @return the global filterOptions
	 */
	public String getDefaultFilterOptions() {
		return PluginConfig.defaultFilterOptions;
	}

	/**
	 * @param defaultFilterOptions the filter options to set
	 */
	public void setDefaultFilterOptions(String defaultFilterOptions) {
		PluginConfig.defaultFilterOptions = defaultFilterOptions;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, refreshTime, filter, filterOptions);
	}

	@Override
	public boolean equals(Object that) {
		return Objects.equals(this, that);
	}

	/**
	 * Check the correctness of the values contained in this objects:
	 * <UL>
	 * 	<LI>Non empty ID
	 * 	<LI>positive refresh time
	 * </ul>
	 *
	 * @return <code>true</code> if the data contained in this object
	 * 			are correct
	 */
	public boolean isValid() {
		if (id==null || id.isEmpty()) {
			logger.error("Invalid null or empty value ID");
			return false;
		}
		if (refreshTime<=0) {
			logger.error("Invalid refreshTime {}",refreshTime);
			return false;
		}
		logger.debug("Value {} configuration is valid",id);
		return true;
	}


}

