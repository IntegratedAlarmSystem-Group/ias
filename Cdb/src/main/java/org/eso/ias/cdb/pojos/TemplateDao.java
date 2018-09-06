package org.eso.ias.cdb.pojos;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The DAO for the template needed to support replication
 * of identical devices.
 * 
 * @author acaproni
 *
 */
@Entity
@Table(name = "TEMPLATE_DEF")
public class TemplateDao {

	/**
	 * The unique identifier of the template
	 */
	@Id
	@Column(name = "template_id")
	private String id;
	
	/**
	 * The minimal index of replicated identical devices, inclusive
	 */
	@Basic(optional=false)
	private int min;
	
	/**
	 * The maximal index of replicated identical devices, inclusive
	 */
	@Basic(optional=false)
	private int max;
	
	/**
	 * Empty constructor
	 */
	public TemplateDao() {}
	
	/**
	 * Constructor
	 * 
	 * @param The not <code>null</code> nor empty identifier of the template
	 * @param min The minimal index of replicated devices, inclusive
	 * @param max The maximal index of replicated devices, inclusive
	 */
	public TemplateDao(String id, int min, int max) {
		if (id==null || id.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty template ID");
		}
		if (min>=max) {
			throw new IllegalArgumentException("MIN>=MAX is not allowed");
		}
		this.id=id;
		this.min=min;
		this.max=max;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id,min,max);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TemplateDao other = (TemplateDao) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (max != other.max)
			return false;
		if (min != other.min)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TemplateDao [id=" + id + ", min=" + min + ", max=" + max + "]";
	}
}
