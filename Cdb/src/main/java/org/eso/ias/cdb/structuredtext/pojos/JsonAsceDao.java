package org.eso.ias.cdb.structuredtext.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.IasioDao;
import org.eso.ias.cdb.pojos.PropertyDao;

import javax.persistence.Basic;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pojos for JSON that replaces objects inclusion in the ASCE with their IDS.
 *
 * @see AsceDao
 * @author acaproni
 *
 */
public class JsonAsceDao {
	
	/**
	 * The rdb pojo
	 */
	private final AsceDao asce;
	
	/**
	 * The ID of the dasu where this ASCE runs
	 */
	private String dasuID;
	
	/**
	 * The IDs of the IASIOs in input
	 */
	private final Set<String> inputIds;
	
	/**
	 * The ID of the IASIO produced by this ASCE
	 */
	private String outputID;
	
	/**
	 * The ID of the transfer function
	 */
	private String transferFunctionID;

    /**
     * The templated inputs of the ASCE
     */
    @Basic(optional=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
	private final Set<JsonTemplatedInputsDao> templatedInputs= new HashSet<>();
	
	/**
	 * The ID of the template for implementing replication
	 */
	@Basic(optional=true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
	private String templateId;
	
	/**
	 * Empty constructor 
	 */
	public JsonAsceDao() {
		this.asce=new AsceDao();
		this.inputIds= new HashSet<>();
	}
	
	/**
	 * Constructor 
	 * 
	 * @param asce The rdb pojo to mask
	 */
	public JsonAsceDao(AsceDao asce) {
		if (asce==null) {
			throw new NullPointerException("The ASCE pojo can't be null");
		}
		this.asce=asce;
		
		Objects.requireNonNull(this.asce.getDasu(), "Inavlid null DASU");
		dasuID=this.asce.getDasu().getId();
		this.inputIds=asce.getInputs().stream().map(i -> i.getId()).collect(Collectors.toSet());
		Objects.requireNonNull(this.asce.getOutput(), "Inavlid null output IASIO");
		this.outputID=this.asce.getOutput().getId();
		this.transferFunctionID=this.asce.getTransferFunction().getClassName();
		this.templateId=this.asce.getTemplateId();

		asce.getInputs().stream().forEach(iasio -> inputIds.add(iasio.getId()));
        asce.getTemplatedInstanceInputs().stream().forEach( templInput -> {
            templatedInputs.add(
                    new JsonTemplatedInputsDao(
                            templInput.getIasio().getId(),
                            templInput.getTemplateId(),
                            templInput.getInstance()));
        });
	}

	/**
	 * @return The ID of the ASCE
	 * @see AsceDao#getId()
	 */
	public String getId() {
		return asce.getId();
	}

	/**
	 * @param id The ID of the ASCE
	 * @see AsceDao#setId(String)
	 */
	public void setId(String id) {
		asce.setId(id);
	}

	/**
	 * @return the ID of the output
	 * @see AsceDao#getOutput()
	 */
	public String getOutputID() {
		return outputID;
	}

	/**
	 * @return The IDs of th einputs
	 * @see AsceDao#getInputs()
	 */
	public Set<String> getInputIDs() {
		return inputIds;
	}
	
	/**
	 * @param ids The IDs of the inputs
	 * @see AsceDao#getInputs()
	 */
	public void setInputIDs(Set<String> ids) {
		inputIds.addAll(ids);
	}

	/**
	 * Set the ID of the output 
	 * @param id The ID of the output
	 * @see AsceDao#setOutput(IasioDao)
	 */
	public void setOutputID(String id) {
		this.outputID=id;
	}

	/**
	 * Get the properties
	 * 
	 * @return The properties
	 * @see AsceDao#getProps()
	 */
	public Set<PropertyDao> getProps() {
		return asce.getProps();
	}

	/**
	 * Set the ID of the DASU
	 * 
	 * @param id The ID of the DASU where this ASCE runs
	 */
	public void setDasuID(String id) {
		dasuID=id;
	}

	/**
	 * Get the ID of the DASU
	 * 
	 * @return The ID of the DASU
	 * @see AsceDao#getDasu()
	 */
	public String getDasuID() {
		return dasuID;
	}
	
	/**
	 * toString() prints a human readable version of the ASCE
	 * where linked objects (like DASU, IASIOS..) are represented by their
	 * IDs only.
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("jsonAsceDAO=[ID=");
		ret.append(getId());
		ret.append("], Output=");
		ret.append(outputID);
		ret.append("], Inputs={");
		for (String inputId: inputIds) {
			ret.append(' ');
			ret.append(inputId);
		}
		ret.append("} TF=");
		ret.append(transferFunctionID);
		ret.append(", DASU=");
		ret.append(dasuID);
		ret.append(", Props={");
		for (PropertyDao prop: getProps()) {
			ret.append(' ');
			ret.append(prop.toString());
		}
		ret.append("}");
		if (templateId!=null) {
			ret.append(", template=");
			ret.append(templateId);
		}
		ret.append("]");
		return ret.toString();
	}
	
	/**
	 * Return the {@link AsceDao} encapsulated in this object.
	 * 
	 * @return The AsceDao
	 */
	public AsceDao toAsceDao() {
		asce.setTemplateId(templateId);
		return this.asce;
	}

	public String getTransferFunctionID() {
		return transferFunctionID;
	}

	public void setTransferFunctionID(String transferFunctionID) {
		this.transferFunctionID = transferFunctionID;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

    public Set<JsonTemplatedInputsDao> getTemplatedInputs() { return templatedInputs; }

    public void setTemplatedInputs(Set<JsonTemplatedInputsDao> templInputs) {
	    this.templatedInputs.clear();
	    this.templatedInputs.addAll(templInputs);
    }
}
