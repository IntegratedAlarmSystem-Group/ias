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
	
	/**
	 *  Human readable description of the IASIO
	 */
	@Basic(optional=true)
	private String shortDesc;
	
	/**
	 * The type of the IASIO
	 */
	@Enumerated(EnumType.STRING)
	@Basic(optional=false)
	private IasTypeDao iasType;
	
	/**
	 * The URL with the documentation
	 */
	@Basic(optional=true)
	private String docUrl;
	
	/**
	 * Empty constructor
	 */
	public IasioDao() {}
	
	/**
	 * Constructor
	 * 
	 * @param id The identifier
	 * @param descr The description
	 * @param type The IAS type
	 */
	public IasioDao(String id, String descr, IasTypeDao type, String docUrl) {
		Objects.requireNonNull(id, "The identifier can't be null");
		Objects.requireNonNull(type, "The IAS type can't be null");
		this.id=id;
		this.shortDesc=descr;
		this.iasType=type;
		this.docUrl=docUrl;
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
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IASIO=[ID=");
		ret.append(getId());
		ret.append(", type=");
		ret.append(getIasType().toString());
		ret.append(", desc=\"");
		if (getShortDesc()!=null) { 
			ret.append(getShortDesc());
		}
		ret.append("\", URL=\"");
		if (getDocUrl()!=null) {
			ret.append(getDocUrl());
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

		return 	this.getId().equals(other.getId()) &&
				this.getIasType().equals(other.getIasType()) && 
				Objects.equals(this.getShortDesc(),other.getShortDesc()) &&
				Objects.equals(this.getDocUrl(),other.getDocUrl()) ;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id,iasType,shortDesc,docUrl);
	}

	public String getDocUrl() {
		return docUrl;
	}

	public void setDocUrl(String docUrl) {
		this.docUrl = docUrl;
	}
}
