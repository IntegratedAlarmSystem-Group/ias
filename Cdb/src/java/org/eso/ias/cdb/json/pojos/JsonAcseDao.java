package org.eso.ias.cdb.json.pojos;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.PropertyDao;

/**
 * Pojos for JSON that replaces objects inclusion in the ASCE with their IDS.
 *
 * @see AsceDao
 * @author acaproni
 *
 */
public class JsonAcseDao {
	
	/**
	 * The rdb pojo
	 */
	private final AsceDao asce;
	
	/**
	 * The ID of the dasu where this ASCE runs
	 */
	private String dasuID;
	
	/**
	 * The IDs of the IASIOs i unput
	 */
	private final Set<String> inputIds;
	
	/**
	 * The ID of the IASIO produced by this ASCE
	 */
	private String outputID;
	
	/**
	 * Empty constructor 
	 */
	public JsonAcseDao() {
		this.asce=new AsceDao();
		this.inputIds= new HashSet<>();
	}
	
	/**
	 * Constructor 
	 * 
	 * @param asce The rdb pojo to mask
	 */
	public JsonAcseDao(AsceDao asce) {
		if (asce==null) {
			throw new NullPointerException("The ASCE pojo can't be null");
		}
		this.asce=asce;
		dasuID=this.asce.getDasu().getId();
		this.inputIds=asce.getInputs().stream().map(i -> i.getId()).collect(Collectors.toSet());
		this.outputID=this.asce.getOutput().getId();
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getId()
	 */
	public String getId() {
		return asce.getId();
	}

	/**
	 * @param id
	 * @see org.eso.ias.cdb.pojos.AsceDao#setId(java.lang.String)
	 */
	public void setId(String id) {
		asce.setId(id);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getTfClass()
	 */
	public String getTfClass() {
		return asce.getTfClass();
	}

	/**
	 * @param tfClass
	 * @see org.eso.ias.cdb.pojos.AsceDao#setTfClass(java.lang.String)
	 */
	public void setTfClass(String tfClass) {
		asce.setTfClass(tfClass);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getOutput()
	 */
	public String getOutputID() {
		return outputID;
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getInputs()
	 */
	public Set<String> getInputIDs() {
		return asce.getInputs().stream().map(i -> i.getId()).collect(Collectors.toSet());
	}
	
	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getInputs()
	 */
	public void setInputIDs(Set<String> ids) {
		inputIds.addAll(ids);
	}

	/**
	 * @param output
	 * @see org.eso.ias.cdb.pojos.AsceDao#setOutput(org.eso.ias.cdb.pojos.IasioDao)
	 */
	public void setOutputID(String id) {
		this.outputID=id;
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getProps()
	 */
	public Set<PropertyDao> getProps() {
		return asce.getProps();
	}

	/**
	 * Setter
	 * 
	 * @param id The ID of the DASU where this ASCE runs
	 */
	public void setDasuID(String id) {
		dasuID=id;
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getDasu()
	 */
	public String getDasuID() {
		return dasuID;
	}
	
	/**
	 * </code>toString()</code> prints a human readable version of the ASCE
	 * where linked objects (like DASU, IASIOS..) are represented by their
	 * IDs only.
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("jsonAsceDAO=[ID=");
		ret.append(getId());
		ret.append(", Output=");
		ret.append(outputID);
		ret.append(", Inputs={");
		for (String inputId: inputIds) {
			ret.append(' ');
			ret.append(inputId);
		}
		ret.append("} TF class=");
		ret.append(getTfClass());
		ret.append(", DASU=");
		ret.append(dasuID);
		ret.append(", Props={");
		for (PropertyDao prop: getProps()) {
			ret.append(' ');
			ret.append(prop.toString());
		}
		ret.append("}]");
		return ret.toString();
	}
	
	/**
	 * Return the {@link AsceDao} encapsulated in this object.
	 * 
	 * @return The AsceDao
	 */
	public AsceDao toAsceDao() {
		return this.asce;
	}
}
