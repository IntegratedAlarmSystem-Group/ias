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
		return filter;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}

	/**
	 * @return the filterOptions
	 */
	public String getFilterOptions() {
		return filterOptions;
	}

	/**
	 * @param filterOptions the filterOptions to set
	 */
	public void setFilterOptions(String filterOptions) {
		this.filterOptions = filterOptions;
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
