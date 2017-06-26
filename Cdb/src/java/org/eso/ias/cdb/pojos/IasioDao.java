package org.eso.ias.cdb.pojos;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The pojo for the IASIO
 * 
 * @author acaproni
 *
 */
@Entity
@Table(name = "IASIO")
public class IasioDao {
	@Id
	@Column(name = "io_id")
	private String id;
	
	// Human readable description of the IASIO
	@Basic(optional=true)
	private String shortDesc;
	
	@Enumerated(EnumType.STRING)
	@Basic(optional=false)
	private IasTypeDao iasType;
	
	/**
	 * Expected refresh rate
	 */
	@Basic(optional=false)
	private int refreshRate;
	
	/**
	 * Empty constructor
	 */
	public IasioDao() {}
	
	/**
	 * Constructor
	 * 
	 * @param id The identifier
	 * @param descr The description
	 * @param rate The refresh rate
	 * @param type The IAS type
	 */
	public IasioDao(String id, String descr, int rate, IasTypeDao type) {
		Objects.requireNonNull(id, "The identifier can't be null");
		Objects.requireNonNull(type, "The IAS type can't be null");
		this.id=id;
		this.shortDesc=descr;
		this.refreshRate=rate;
		this.iasType=type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		Objects.requireNonNull(id, "The identifier can't be null");
		String ident = id.trim();
		if (ident.isEmpty()) {
			throw new IllegalArgumentException("The identifier cannot be an empty string");
		}
		this.id = ident;
	}

	public String getShortDesc() {
		return shortDesc;
	}

	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}

	public IasTypeDao getIasType() {
		return iasType;
	}

	public void setIasType(IasTypeDao iasType) {
		Objects.requireNonNull(iasType, "The IAS type can't be null");
		this.iasType = iasType;
	}

	public int getRefreshRate() {
		return refreshRate;
	}

	public void setRefreshRate(int refreshRate) {
		this.refreshRate = refreshRate;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IASIO=[ID=");
		ret.append(getId());
		ret.append(", type=");
		ret.append(getIasType().toString());
		ret.append(", refreshRate=");
		ret.append(getRefreshRate());
		if (getShortDesc()!=null) { 
			ret.append(", desc=\"");		
			ret.append(getShortDesc());
		} else {
			ret.append(", NO description given");
		}
		ret.append("\"]");
		return ret.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}

		IasioDao other = (IasioDao) obj;

		return this.getRefreshRate() == other.getRefreshRate() && 
				this.getId().equals(other.getId()) &&
				this.getIasType().equals(other.getIasType()) && 
				Objects.equals(this.getShortDesc(),other.getShortDesc());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id,iasType,refreshRate,shortDesc);
	}
}
