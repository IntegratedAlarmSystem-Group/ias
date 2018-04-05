package org.eso.ias.cdb.json.pojos;

import java.util.Objects;

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
	private String dasuId;
	
	/**
	 * The ID of the template
	 */
	private String templateId;
	
	/**
	 * The instance of the templated DASU
	 * to deploy
	 */
	private Integer instance;
	
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
		
		// If the DASU is templted then both the template and the instance number
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

	

}
