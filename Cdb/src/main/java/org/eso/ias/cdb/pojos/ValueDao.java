package org.eso.ias.cdb.pojos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The value of a monitor point or alarm of plugin
 * read from the monitored system.
 *
 * @author acaproni
 *
 */
public class ValueDao {

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
		this.filter=filter;		
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
		this.filterOptions=filterOptions;		
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, refreshTime, filter, filterOptions);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValueDao valueDao = (ValueDao) o;
		return refreshTime == valueDao.refreshTime &&
				id.equals(valueDao.id) &&
				Objects.equals(filter, valueDao.filter) &&
				Objects.equals(filterOptions, valueDao.filterOptions);
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
	@JsonIgnore
	public boolean valid() {
		if (id==null || id.isEmpty()) {
			return false;
		}
		if (refreshTime<=0) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("ValueDao[id=");
		ret.append(id);
		ret.append(", refreshTime=");
		ret.append(refreshTime);
		if (filter!=null && !filter.isEmpty()) {
			ret.append(", filter=");
			ret.append(filter);
		}
		if (filterOptions!=null && !filterOptions.isEmpty()) {
			ret.append(", filterOptions=");
			ret.append(filterOptions);
		}
		ret.append(']');
		return ret.toString();
	}
}

