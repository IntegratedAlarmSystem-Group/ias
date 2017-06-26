package org.eso.ias.cdb.json.pojos;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.LogLevelDao;

/**
 * Pojos for JSON that replaces object inclusion with their IDS.
 *
 * @see DasuDao
 * @author acaproni
 *
 */
public class JsonDasuDao {
	
	/**
	 * The rdb pojo
	 */
	private final DasuDao dasu;
	
	/**
	 * The ID of the supervisor where this DASU run
	 */
	private String supervisorId;
	
	/**
	 * The IDs of the ASCEs that run into this DASU
	 */
	private final Set<String> asceIDs;
	
	/**
	 * Constructor
	 * 
	 * @param dasu The rdb pojo to mask
	 */
	public JsonDasuDao(DasuDao dasu) {
		if (dasu==null) {
			throw new NullPointerException("The DASU pojo can't be null");
		}
		this.dasu=dasu;
		this.asceIDs=dasu.getAsces().stream().map(i -> i.getId()).collect(Collectors.toSet());
		this.supervisorId=dasu.getSupervisor().getId();
	}
	
	/**
	 * Empty constructor
	 */
	public JsonDasuDao() {
		this.dasu=new DasuDao();
		this.asceIDs=new HashSet<>();
	}

	/**
	 * Return the log level of the DASU
	 * 
	 * @return The log level of teh DASU
	 * @see DasuDao#getLogLevel()
	 */
	public LogLevelDao getLogLevel() {
		return dasu.getLogLevel();
	}

	/**
	 * Set the log level 
	 * 
	 * @param logLevel The log level to set
	 * @see DasuDao#setLogLevel(LogLevelDao)
	 */
	public void setLogLevel(LogLevelDao logLevel) {
		dasu.setLogLevel(logLevel);
	}

	/**
	 * Get the ID of the DASU
	 * 
	 * @return the ID of the DASU
	 * @see DasuDao#getId()
	 */
	public String getId() {
		return dasu.getId();
	}

	/**
	 * Get the Id of the supervisor where this DASU is deployed
	 * 
	 * @return the Id of the supervisor
	 * @see DasuDao#getSupervisor()
	 */
	public String getSupervisorID() {
		return supervisorId;
	}
	
	/**
	 * Set the ID of the Supervisor
	 * @param supervID the ID of the Supervisor
	 */
	public void setSupervisorID(String supervID) {
		supervisorId=supervID;
	}

	/**
	 * Set the ID of the DASU
	 * 
	 * @param id the ID of the DASU
	 * @see DasuDao#setId(java.lang.String)
	 */
	public void setId(String id) {
		dasu.setId(id);
	}

	/**
	 * Get the IDs of the ASCEs running in this DASU
	 * @return the IDs of the ASCEs
	 * @see org.eso.ias.cdb.pojos.DasuDao#getAsces()
	 */
	public Set<String> getAsceIDs() {
		return asceIDs;
	}
	
	/**
	 * Set the IDs of the ASCEs running in this DASU
	 * 
	 * @param ids the IDs of the ASCEs running in this DASU
	 */
	public void setAsceIDs(Set<String> ids) {
		asceIDs.addAll(ids);
	}
	
	/**
	 * toString() prints a human readable version of the DASU
	 * where linked objects (like ASCES) are represented by their
	 * IDs only.
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("jsonDasuAO=[ID=");
		ret.append(getId());
		ret.append(", logLevel=");
		ret.append(getLogLevel());
		ret.append(", Supervisor=");
		ret.append(getSupervisorID());
		ret.append(", ASCEs={");
		for (String asce: getAsceIDs()) {
			ret.append(" ");
			ret.append(asce);
		}
		ret.append("}]");
		return ret.toString();
	}
	
	/**
	 * @return the {@link DasuDao} encapsulated in this object.
	 */
	public DasuDao toDasuDao() {
		return this.dasu;
	
	}
}
