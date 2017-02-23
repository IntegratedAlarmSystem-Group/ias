package org.eso.ias.cdb.json.pojos;

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
	 * Constructor
	 * 
	 * @param dasu The rdb pojo to mask
	 */
	public JsonDasuDao(DasuDao dasu) {
		if (dasu==null) {
			throw new NullPointerException("The DASU pojo can't be null");
		}
		this.dasu=dasu;
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
		return dasu.getSupervisor().getId();
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
		return dasu.getAsces().stream().map(i -> i.getId()).collect(Collectors.toSet());
	}
}
