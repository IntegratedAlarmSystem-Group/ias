package org.eso.ias.cdb.json.pojos;

import java.util.Set;
import java.util.stream.Collectors;

import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * <code>JsonSupervisorDao</code> encapsulate a {@link SupervisorDao}
 * to replaces its set of DASUs with a set of IDs.
 * 
 * @author acaproni
 *
 */
public class JsonSupervisorDao {
	
	/**
	 * The SupervisorDao to make its DASUs
	 */
	private final SupervisorDao supervisorDao;
	
	/**
	 * Constructor
	 * 
	 * @param supervisorDao The CDB supervisor pojo
	 */
	public JsonSupervisorDao(SupervisorDao supervisorDao) {
		if (supervisorDao==null) {
			throw new NullPointerException("The SupervisorDao can't be null");
		}
		this.supervisorDao=supervisorDao;
	}
	
	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return supervisorDao.equals(obj);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.SupervisorDao#getId()
	 */
	public String getId() {
		return supervisorDao.getId();
	}

	/**
	 * @param id
	 * @see org.eso.ias.cdb.pojos.SupervisorDao#setId(java.lang.String)
	 */
	public void setId(String id) {
		supervisorDao.setId(id);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.SupervisorDao#getHostName()
	 */
	public String getHostName() {
		return supervisorDao.getHostName();
	}

	/**
	 * @param host
	 * @see org.eso.ias.cdb.pojos.SupervisorDao#setHostName(java.lang.String)
	 */
	public void setHostName(String host) {
		supervisorDao.setHostName(host);
	}

	/**
	 * @return
	 * @see org.eso.ias.cdb.pojos.SupervisorDao#getLogLevel()
	 */
	public LogLevelDao getLogLevel() {
		return supervisorDao.getLogLevel();
	}

	/**
	 * @param logLevel
	 * @see org.eso.ias.cdb.pojos.SupervisorDao#setLogLevel(org.eso.ias.cdb.pojos.LogLevelDao)
	 */
	public void setLogLevel(LogLevelDao logLevel) {
		supervisorDao.setLogLevel(logLevel);
	}
	
	/**
	 * Replaces DASUs in {@link #supervisorDao} with their IDs
	 * 
	 * @return The IDs of the DASUs of this supervisor
	 */
	public Set<String> getDasusIDs() {
		return supervisorDao.getDasus().stream().map(i -> i.getId()).collect(Collectors.toSet());
	}
}
