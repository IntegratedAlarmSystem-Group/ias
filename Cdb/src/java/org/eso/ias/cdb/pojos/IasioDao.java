package org.eso.ias.cdb.pojos;

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
	private int refreshRate;
	
	public IasioDao() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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
		this.iasType = iasType;
	}

	public int getRefreshRate() {
		return refreshRate;
	}

	public void setRefreshRate(int refreshRate) {
		this.refreshRate = refreshRate;
	}
	
	
}
