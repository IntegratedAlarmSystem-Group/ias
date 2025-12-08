package org.eso.ias.cdb.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;
import java.util.Objects;

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
	@JsonInclude(JsonInclude.Include.NON_NULL)
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
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String docUrl=null;
	
	/**
	 * The ID of the template for implementing replication
	 */
	@Basic(optional=true)
	@Column(name = "template_id")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String templateId=null;
	
	/**
	 * The default value for canShelve: by default no
	 * alarms can be shelved
	 */
	public static final boolean canSheveDefault = false;
	
	/**
	 * The attribute saying if a IASIO can be shelved,
	 * initialized with the default value {@link #canSheveDefault}
	 */
	@Basic(optional=true)
	private boolean canShelve=canSheveDefault;
	
	/**
	 * The sound to play when a given alarm becomes SET
	 * 
	 * This attribute is ignored for non alarm IASIOs
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "Sound")
	@Basic(optional=true)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private SoundTypeDao sound=SoundTypeDao.NONE;
	
	/**
	 * The addresses to send emails when an alarm changes
	 * it state SET/CLEAR
	 * 
	 * This attribute is ignored for non alarm IASIOs
	 */
	@Column(name = "emails")
	@Basic(optional=true)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String emails;

	/**
	 * Empty constructor
	 */
	public IasioDao() {}
	
	/**
	 * Constructor with default canShelve 
	 * 
	 * @param id The identifier
	 * @param descr The description
	 * @param type The IAS type
	 * @param docUrl the URL of the documentation
	 */
	public IasioDao(String id, String descr, IasTypeDao type, String docUrl) {
		Objects.requireNonNull(id, "The identifier can't be null");
		Objects.requireNonNull(type, "The IAS type can't be null");
		this.id=id;
		this.shortDesc=descr;
		this.iasType=type;
		this.docUrl=docUrl;
	}
	
	/**
	 * Constructor
	 * 
	 * @param id The identifier
	 * @param descr The description
	 * @param type The IAS type
	 * @param docUrl the URL of the documentation
	 * @param canShelve <code>true</code> if this IASIO can be shelved, 
	 *                  <code>false</code> otherwise
	 * @param templateId the Id of the template for replication
	 * @param sound the sound to play when an alarm is set
	 * @param emails the emails to notify when an alarm is SET or CLEARED
	 */
	public IasioDao(
			String id, 
			String descr, 
			IasTypeDao type, 
			String docUrl, 
			boolean canShelve, 
			String templateId,
			SoundTypeDao sound,
			String emails) {
		this(id,descr,type,docUrl);
		this.canShelve=canShelve;
		this.templateId=templateId;
		if (sound ==null) {
			this.sound = SoundTypeDao.NONE;
		} else {
			this.sound = sound;
		}
		this.emails = emails;
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
		if (getShortDesc()!=null && !getShortDesc().isEmpty()) {
			ret.append(", desc=\"");
			ret.append(getShortDesc());
			ret.append('"');
		}
		if (getDocUrl()!=null && !getDocUrl().isEmpty()) {
			ret.append(", URL=\"");
			ret.append(getDocUrl());
			ret.append('"');
		}
		if (canShelve) {
			ret.append(", can be shelved");
		} else {
			ret.append(", cannot be shelved");
		}
		if (templateId!=null) {
			ret.append(", template id=\"");
			ret.append(templateId);
			ret.append('"');
		}
		if (sound!=null) {
			ret.append(", sound type=");
			ret.append(sound);
		}
		if (emails!=null && !emails.isEmpty()) {
			ret.append(", emails=\"");
			ret.append(emails);
			ret.append('"');
		}
		ret.append("]");
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
				Objects.equals(this.getDocUrl(),other.getDocUrl()) &&
				this.isCanShelve()==other.isCanShelve() &&
				Objects.equals(this.getTemplateId(),other.getTemplateId()) &&
				Objects.equals(this.getEmails(),other.getEmails()) &&
				Objects.equals(this.getSound(),other.getSound());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id,iasType,shortDesc,docUrl,canShelve,templateId,sound,emails);
	}

	public String getDocUrl() {
		return docUrl;
	}

	public void setDocUrl(String docUrl) {
		this.docUrl = docUrl;
	}

	public boolean isCanShelve() {
		return canShelve;
	}

	public void setCanShelve(boolean canShelve) {
		this.canShelve = canShelve;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public SoundTypeDao getSound() {
		return sound;
	}

	public void setSound(SoundTypeDao sound) {
		if (sound ==null) {
			this.sound = SoundTypeDao.NONE;
		} else {
			this.sound = sound;
		}
	}

	public String getEmails() {
		return emails;
	}

	public void setEmails(String emails) {
		this.emails = emails;
	}
}
