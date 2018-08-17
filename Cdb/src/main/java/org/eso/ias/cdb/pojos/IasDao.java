package org.eso.ias.cdb.pojos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Global configuration for the IAS
 * 
 * @author acaproni
 */
@Entity
@Table(name = "IAS")
public class IasDao {
	
	@Id
	@SequenceGenerator(name="IAS_SEQ_GENERATOR", sequenceName="IAS_SEQ_GENERATOR", allocationSize=1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="IAS_SEQ_GENERATOR")
	@Column(name = "id")
    private Long id;

	/**
	 * The log level
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "logLevel")
	private LogLevelDao logLevel;
	
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinTable(name= "IAS_PROPERTY",
			joinColumns = @JoinColumn(name="Ias_id"),
			inverseJoinColumns = @JoinColumn(name = "props_id"))
	private Set<PropertyDao> props = new HashSet<>();
	
	/**
	 * Expected refresh rate
	 */
	@Basic(optional=false)
	private int refreshRate;
	
	/**
	 * The tolerance added by clients to the refresh rate
	 * to invalidate a monitor point
	 */
	@Basic(optional=false)
	private int tolerance;
	
	/**
	 * The frequency of the heartbeat sent by each IAS tool
	 * in seconds
	 */
	@Basic(optional=false)
	private int hbFrequency;
	
	/**
	 * The URL to connect to the BSDB.
	 * 
	 * In case of kafka it is a comma separated list of server:port
	 */
	@Basic(optional=false)
	private String bsdbUrl;

	/**
	 * The string to connect to the SMTP to send emails
	 * Format username:password@hostname
	 */
	@Basic(optional = true)
	private String smtp=null;
	
	/**
	 * Empty constructor
	 */
	public IasDao() {}
	
	public LogLevelDao getLogLevel() {
		return this.logLevel;
	}
	
	public void setLogLevel(LogLevelDao logLevel) {
		this.logLevel=logLevel;
	}
	
	public Set<PropertyDao> getProps() {
		return props;
	}
	
	public int getRefreshRate() {
		return refreshRate;
	}
	
	public void setRefreshRate(int refreshRate) {
		this.refreshRate = refreshRate;
	}
	
	public int getTolerance() {
		return tolerance;
	}
	
	public void setTolerance(int tolerance) {
		this.tolerance = tolerance;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("IAS=[");
		ret.append("logLevel=");
		ret.append(getLogLevel().toString());
		ret.append(", refreshRate=");
		ret.append(refreshRate);
		ret.append(", tolerance=");
		ret.append(tolerance);
		ret.append(", heartebeat frequency=");
		ret.append(hbFrequency);
		ret.append(", BSDB URL=`");
		ret.append(bsdbUrl);
		if (smtp!=null && !smtp.isEmpty()) {
			ret.append("`, SMTP=`");
			ret.append(smtp);
		}
		ret.append("`, props={");
		for (PropertyDao prop: getProps()) {
			ret.append(' ');
			ret.append(prop.toString());
		}
		ret.append("}]");
		return ret.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(logLevel,refreshRate,tolerance,props,hbFrequency,bsdbUrl,smtp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IasDao other = (IasDao) obj;
		
		return 	Objects.equals(logLevel, other.logLevel) &&
				Objects.equals(refreshRate, other.refreshRate) &&
				Objects.equals(tolerance, other.tolerance) &&
				Objects.equals(hbFrequency, other.hbFrequency) &&
				Objects.equals(bsdbUrl, other.bsdbUrl) &&
				Objects.equals(smtp, other.smtp) &&
				Objects.equals(props, other.props);
	}

	public int getHbFrequency() {
		return hbFrequency;
	}

	public void setHbFrequency(int heartbeatFrequency) {
		this.hbFrequency = heartbeatFrequency;
	}

	public String getBsdbUrl() {
		return bsdbUrl;
	}

	public void setBsdbUrl(String bsdbUrl) {
		this.bsdbUrl = bsdbUrl;
	}

	public String getSmtp() {
		return smtp;
	}

	public void setSmtp(String smtp) {
		this.smtp = smtp;
	}
}
