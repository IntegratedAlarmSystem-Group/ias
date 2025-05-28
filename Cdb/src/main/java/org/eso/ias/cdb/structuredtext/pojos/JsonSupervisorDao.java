package org.eso.ias.cdb.structuredtext.pojos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

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
	 * The Supervisor let a DASU process the inputs in a thread so that
	 * all the outputs can be produced concurrently (#248).
	 * 
	 * Ideally there should be one thread for each DASU in the Supervisor 
	 * but we can limit the number of threads to avoid
	 * the creation of too many threads in the same process.
	 * 
	 * In a reasonable deployment, the Supervisor should not run too many DASUs
	 * so the default value of 512 seems at this stage more than enough.
	 * 
	 * The minimum number of threads is to 32.
	 */
	@Basic(optional=true)
	private int maxThreads = 512;
	
	/**
	 * The time in milliseconds to wait between consecutive running of a DASU.
	 * 
	 * The Supervisor collects the inputs during this time, then runs the DASU against
	 * the collected inputs. 
	 * 
	 * It might be reasonable to increase the value from the default but special care should 
	 * be taken using shorter time intervals as during that short time it is possible that 
	 * not all the inputs have been arrived and the recalculation is practically useless.
	 * 
	 * A value littler then 100ms is not recommended as it can lead to
	 * a very high CPU usage.
	 * The min accepted value is 50ms.
	 */
	@Basic(optional=true)
	private int throttlingTime = 250; // in milliseconds, default 250ms
	
	/**
	 * The DASUs are replaced by their IDs
	 */
	private Set<JsonDasuToDeployDao>dasusToDeploy;
	
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
		maxThreads = supervisorDao.getMaxThreads();
		throttlingTime = supervisorDao.getThrottlingTime();
		this.dasusToDeploy=supervisorDao.getDasusToDeploy().stream().map(i -> new JsonDasuToDeployDao(i)).collect(Collectors.toSet());
	}

	public String toString() {
		StringBuilder ret = new StringBuilder("JsonSupervisorDAO=[ID=");
		ret.append(getId());
		ret.append(", logLevel=");
		ret.append(getLogLevel());
		ret.append(", hostName=");
		ret.append(getHostName());
		ret.append(", maxThreads=");
		ret.append(maxThreads);
		ret.append(", throttlingTime=");
		ret.append(throttlingTime);
		ret.append(", DASUs={");
		for (JsonDasuToDeployDao jdtd : getDasusToDeploy()) {
			ret.append(" ");
			ret.append(jdtd);
		}
		ret.append("}]");
		return ret.toString();
	}

	public Set<JsonDasuToDeployDao> getDasusToDeploy() {
		if (dasusToDeploy==null) {
			dasusToDeploy = new HashSet<>();
		}
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

	public int getMaxThreads() {
		return maxThreads;
	}
	public void setMaxThreads(int maxThreads) {
		if (maxThreads<32) {
			throw new IllegalArgumentException("The minimum number of threads must be at least 32");
		}
		this.maxThreads = maxThreads;
	}
	public int getThrottlingTime() {
		return throttlingTime;
	}
	public void setThrottlingTime(int throttlingTime) {
		if (throttlingTime<50) {
			throw new IllegalArgumentException("The minimum throttling time must be at least 50ms");
		}
		this.throttlingTime = throttlingTime;
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
