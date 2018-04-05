package org.eso.ias.cdb.json.pojos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eso.ias.cdb.pojos.DasuDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.pojos.SupervisorDao;

/**
 * JsonSupervisorDao encapsulate a {@link SupervisorDao}
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
	private final Set<JsonDasuToDeployDao>dasusToDeploy;
	
	/**
	 * Empty constructor.
	 * 
	 * This constructor is needed while deserializing.
	 * 
	 */
	public JsonSupervisorDao() {
		supervisorDao = new SupervisorDao();
		this.dasusToDeploy = new HashSet<>();
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
		this.dasusToDeploy=supervisorDao.getDasusToDeploy().stream().map(i -> new JsonDasuToDeployDao(i)).collect(Collectors.toSet());
	}
	
	/**
	 * @param obj The object to check
	 * @return <code>true</code> if this object is equal to the passed object
	 * @see java.lang.Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonSupervisorDao other =(JsonSupervisorDao)obj;
		return this.getId().equals(other.getId()) && 
				this.getHostName().equals(other.getHostName()) &&
				this.getLogLevel().equals(other.getLogLevel()) &&
				this.getDasusToDeployIDs().equals(other.getDasusToDeployIDs());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getId(),getHostName(),getLogLevel(),getDasusToDeployIDs());
	}

	/**
	 * Get the ID of the Supervisor
	 * 
	 * @return the ID of the Supervisor
	 * @see SupervisorDao#getId()
	 */
	public String getId() {
		return supervisorDao.getId();
	}

	/**
	 * Set the ID of the Supervisor
	 * 
	 * @param id the ID of the Supervisor
	 * @see SupervisorDao#setId(java.lang.String)
	 */
	public void setId(String id) {
		supervisorDao.setId(id);
	}

	/**
	 * Get the name of the host
	 * 
	 * @return the name of the host
	 * @see SupervisorDao#getHostName()
	 */
	public String getHostName() {
		return supervisorDao.getHostName();
	}

	/**
	 * Set the name of the host
	 * 
	 * @param host the name of the host
	 * @see SupervisorDao#setHostName(java.lang.String)
	 */
	public void setHostName(String host) {
		supervisorDao.setHostName(host);
	}

	/**
	 * Get the log level
	 * 
	 * @return the log level
	 * @see SupervisorDao#getLogLevel()
	 */
	public LogLevelDao getLogLevel() {
		return supervisorDao.getLogLevel();
	}

	/**
	 * Set the log level
	 * @param logLevel the log level
	 * @see SupervisorDao#setLogLevel(LogLevelDao)
	 */
	public void setLogLevel(LogLevelDao logLevel) {
		supervisorDao.setLogLevel(logLevel);
	}
	
	/**
	 * Get the IDs of the DASUs
	 * 
	 * @return The IDs of the DASUs of this supervisor
	 */
	public Set<String> getDasusToDeployIDs() {
		return dasusToDeploy.stream().map(dtd -> dtd.getDasuId()).collect(Collectors.toSet());
	}
	
	public String toString() {
		StringBuilder ret = new StringBuilder("JsonSupervisorDAO=[ID=");
		ret.append(getId());
		ret.append(", logLevel=");
		ret.append(getLogLevel());
		ret.append(", hostName=");
		ret.append(getHostName());
		ret.append(", DASUs={");
		for (String dtdId : getDasusToDeployIDs()) {
			ret.append(" ");
			ret.append(dtdId);
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
