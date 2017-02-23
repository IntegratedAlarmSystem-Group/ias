package org.eso.ias.cdb.json.pojos;

import java.util.Set;
import java.util.stream.Collectors;

import org.eso.ias.cdb.pojos.AsceDao;
import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.IasioDao;
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
	
	public JsonAcseDao(AsceDao asce) {
		if (asce==null) {
			throw new NullPointerException("The ASCE pojo can't be null");
		}
		this.asce=asce;
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
		return asce.getOutput().getId();
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getInputs()
	 */
	public Set<String> getInputIDs() {
		return asce.getInputs().stream().map(i -> i.getId()).collect(Collectors.toSet());
	}

	/**
	 * @param output
	 * @see org.eso.ias.cdb.pojos.AsceDao#setOutput(org.eso.ias.cdb.pojos.IasioDao)
	 */
	public void setOutput(IasioDao output) {
		asce.setOutput(output);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getProps()
	 */
	public Set<PropertyDao> getProps() {
		return asce.getProps();
	}

	/**
	 * @param dasu
	 * @see org.eso.ias.cdb.pojos.AsceDao#setDasu(org.eso.ias.cdb.pojos.DasuDao)
	 */
	public void setDasu(DasuDao dasu) {
		asce.setDasu(dasu);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.AsceDao#getDasu()
	 */
	public String getDasuID() {
		return asce.getDasu().getId();
	}
}
