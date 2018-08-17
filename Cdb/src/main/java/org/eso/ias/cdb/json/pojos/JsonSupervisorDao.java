package org.eso.ias.cdb.json.pojos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

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
	 * Supervisor ID
	 */
	@Basic(optional=false)
	private String id;
	
	/**
	 * The host where the Supervisor runs
	 */
	@Basic(optional=false)
	private String hostName;
	
	/**
	 * The log level
	 * 
	 * A Supervisor inherits the IAS log level if undefined in the CDB.
	 */
	@Enumerated(EnumType.STRING)
	@Basic(optional=true)
	private LogLevelDao logLevel;
	
	/**
	 * The DASUs are replaced by their IDs
	 */
	private Set<JsonDasuToDeployDao>dasusToDeploy = new HashSet<>();
	
	/**
	 * Empty constructor.
	 * 
	 * This constructor is needed while deserializing.
	 * 
	 */
	public JsonSupervisorDao() {
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
		id = supervisorDao.getId();
		hostName = supervisorDao.getHostName();
		logLevel = supervisorDao.getLogLevel();
		this.dasusToDeploy=supervisorDao.getDasusToDeploy().stream().map(i -> new JsonDasuToDeployDao(i)).collect(Collectors.toSet());
	}

	public String toString() {
		StringBuilder ret = new StringBuilder("JsonSupervisorDAO=[ID=");
		ret.append(getId());
		ret.append(", logLevel=");
		ret.append(getLogLevel());
		ret.append(", hostName=");
		ret.append(getHostName());
		ret.append(", DASUs={");
		for (JsonDasuToDeployDao jdtd : getDasusToDeploy()) {
			ret.append(" ");
			ret.append(jdtd);
		}
		ret.append("}]");
		return ret.toString();
	}

	public Set<JsonDasuToDeployDao> getDasusToDeploy() {
		return dasusToDeploy;
	}
	
	
	public void addDasusToDeploy(JsonDasuToDeployDao jdtd) {
		Objects.requireNonNull(jdtd);
		if (dasusToDeploy==null) {
			dasusToDeploy = new HashSet<>();
		}
		// The DTD valid?
		if (jdtd.getDasuId()==null || jdtd.getDasuId().isEmpty()) {
			throw new IllegalArgumentException("Invalid jDTD ID");
		}
		if ((jdtd.getInstance()==null && jdtd.getTemplateId()!=null) ||
		(jdtd.getInstance()!=null && jdtd.getTemplateId()==null)) {
			throw new IllegalArgumentException("Inconsistent template defitinion: "+jdtd.toString());
		}
		dasusToDeploy.add(jdtd);
	}

	public void setDasusToDeploy(Set<JsonDasuToDeployDao> dasusToDeploy) {
		this.dasusToDeploy = dasusToDeploy;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public LogLevelDao getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(LogLevelDao logLevel) {
		this.logLevel = logLevel;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dasusToDeploy == null) ? 0 : dasusToDeploy.hashCode());
		result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((logLevel == null) ? 0 : logLevel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonSupervisorDao other = (JsonSupervisorDao) obj;
		if (dasusToDeploy == null) {
			if (other.dasusToDeploy != null)
				return false;
		} else if (!dasusToDeploy.equals(other.dasusToDeploy))
			return false;
		if (hostName == null) {
			if (other.hostName != null)
				return false;
		} else if (!hostName.equals(other.hostName))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (logLevel != other.logLevel)
			return false;
		return true;
	}
}
