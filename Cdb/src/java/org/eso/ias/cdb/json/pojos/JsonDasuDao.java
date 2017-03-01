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
	 * @return
	 * @see org.eso.ias.cdb.pojos.DasuDao#getLogLevel()
	 */
	public LogLevelDao getLogLevel() {
		return dasu.getLogLevel();
	}

	/**
	 * @param logLevel
	 * @see org.eso.ias.cdb.pojos.DasuDao#setLogLevel(org.eso.ias.cdb.pojos.LogLevelDao)
	 */
	public void setLogLevel(LogLevelDao logLevel) {
		dasu.setLogLevel(logLevel);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.DasuDao#getId()
	 */
	public String getId() {
		return dasu.getId();
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.DasuDao#getSupervisor()
	 */
	public String getSupervisorID() {
		return supervisorId;
	}
	
	/**
	 * Setter
	 */
	public void setSupervisorID(String supervID) {
		supervisorId=supervID;
	}

	/**
	 * @param id
	 * @see org.eso.ias.cdb.pojos.DasuDao#setId(java.lang.String)
	 */
	public void setId(String id) {
		dasu.setId(id);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.DasuDao#getAsces()
	 */
	public Set<String> getAsceIDs() {
		return asceIDs;
	}
	
	/**
	 * Setter
	 */
	public void setAsceIDs(Set<String> ids) {
		asceIDs.addAll(ids);
	}
	
	/**
	 * </code>toString()</code> prints a human readable version of the DASU
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
//		
	}
}
