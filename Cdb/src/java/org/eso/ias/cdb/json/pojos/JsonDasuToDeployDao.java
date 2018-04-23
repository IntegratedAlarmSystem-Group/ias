package org.eso.ias.cdb.json.pojos;

import java.util.Objects;

import javax.persistence.Basic;

import org.eso.ias.cdb.pojos.DasuToDeployDao;
import org.eso.ias.cdb.pojos.DasuDao;
/**
 * The DASU to deploy to replace the reference to the DASU 
 * ant the template with their IDs
 * 
 * @author acaproni
 *
 */
public class JsonDasuToDeployDao {
	
	/**
	 * The ID of the DASU
	 */
	@Basic(optional=false)
	private String dasuId;
	
	/**
	 * The ID of the template
	 */
	@Basic(optional=true)
	private String templateId=null;
	
	/**
	 * The instance of the templated DASU
	 * to deploy
	 */
	@Basic(optional=true)
	private Integer instance=null;
	
	/**
	 * Empty constructor
	 */
	public JsonDasuToDeployDao() {}
	
	/**
	 * Constructor
	 * 
	 * @param dasuId The ID of the DASU to deploy
	 * @param templateId The template of the DASU, if templated
	 * @param number The instance of the DASU, if templated
	 */
	public JsonDasuToDeployDao(String dasuId, String templateId, Integer number) {
		super();
		if (dasuId==null || dasuId.isEmpty()) {
			throw new IllegalArgumentException("Invalid null or empty invalid DASU ID");
		}
		
		// If the DASU is templated then both the template and the instance number
		// must be defined
		//
		// If it is not templated then both template and number must be null
		assert ((templateId==null && number==null) || (templateId!=null && number!=null));
		
		this.dasuId = dasuId;
		this.templateId = templateId;
		this.instance = number;
	}

	/**
	 * Constructor
	 * 
	 * @param dtd The not <code>null</code> dasu to deploy
	 */
	public JsonDasuToDeployDao(DasuToDeployDao dtd) {
		super();
		Objects.requireNonNull(dtd);
		
		String id = dtd.getDasu().getId();
		String tempId = (dtd.getTemplate()!=null)?dtd.getTemplate().getId():null;
		Integer num = dtd.getInstance();
		
		// If the DASU is templted then both the template and the instance number
		// must be defined
		//
		// If it is not templated then both template and number must be null
		assert ((tempId==null && num==null) || (tempId!=null && num!=null));
		
		this.dasuId=id;
		this.templateId=tempId;
		this.instance=num;
	}
	
	public String getDasuId() {
		return dasuId;
	}

	public void setDasuId(String dasuId) {
		this.dasuId = dasuId;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public Integer getInstance() {
		return instance;
	}

	public void setInstance(Integer instance) {
		this.instance = instance;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dasuId == null) ? 0 : dasuId.hashCode());
		result = prime * result + ((instance == null) ? 0 : instance.hashCode());
		result = prime * result + ((templateId == null) ? 0 : templateId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonDasuToDeployDao other = (JsonDasuToDeployDao) obj;
		if (dasuId == null) {
			if (other.dasuId != null)
				return false;
		} else if (!dasuId.equals(other.dasuId))
			return false;
		if (instance == null) {
			if (other.instance != null)
				return false;
		} else if (!instance.equals(other.instance))
			return false;
		if (templateId == null) {
			if (other.templateId != null)
				return false;
		} else if (!templateId.equals(other.templateId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JsonDasuToDeployDao [dasuId=" + dasuId + ", templateId=" + templateId + ", instance=" + instance + "]";
	}

	

}
