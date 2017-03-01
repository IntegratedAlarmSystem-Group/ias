package org.eso.ias.cdb.json.pojos;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eso.ias.cdb.pojos.DasuDao;
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
	 * The DASUs are replaced by their IDs
	 */
	private final Set<String>dasuIDs;
	
	/**
	 * Empty constructor.
	 * 
	 * This constructor is needed while deserializing.
	 * 
	 */
	public JsonSupervisorDao() {
		supervisorDao = new SupervisorDao();
		this.dasuIDs = new HashSet<>();
	}
	
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
		this.dasuIDs=supervisorDao.getDasus().stream().map(i -> i.getId()).collect(Collectors.toSet());
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
		return dasuIDs;
	}
	
	/**
	 * Replaces DASUs in {@link #supervisorDao} with their IDs
	 * 
	 * @return The IDs of the DASUs of this supervisor
	 */
	public void setDasusIDs(Set<String> ids) {
		this.dasuIDs.addAll(ids);
	}
	
	public String toString() {
		StringBuilder ret = new StringBuilder("JsonSupervisorDAO=[ID=");
		ret.append(getId());
		ret.append(", logLevel=");
		ret.append(getLogLevel());
		ret.append(", hostName=");
		ret.append(getHostName());
		ret.append(", DASUs={");
		for (String dasuId : getDasusIDs()) {
			ret.append(" ");
			ret.append(dasuId);
		}
		ret.append("}]");
		return ret.toString();
	}
	
	/**
	 * Return the {@link DasuDao} encapsulated in this object.
	 * 
	 * @return The DasuDao
	 */
	public SupervisorDao toSupervisorDao() {
		return this.supervisorDao;
//		for (String dasuId: dasuIDs) {
//			if (!supervisorDao.containsDasu(dasuId)) {
//				try {
//					Optional<DasuDao> optDasu = reader.getDasu(dasuId);
//					if (!optDasu.isPresent()) {
//						return Optional.empty();
//					}
//				} catch (Throwable t) {
//					System.err.println("Error getting DASU "+dasuId);
//					t.printStackTrace();
//					return Optional.empty();
//				}
//			}
//		}
//		return Optional.of(supervisorDao);
	}
}
