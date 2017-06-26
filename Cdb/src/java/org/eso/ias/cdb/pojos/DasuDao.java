package org.eso.ias.cdb.pojos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * The pojo for a DASU
 * 
 * @author acaproni
 *
 */
@Entity(name = "DASU")
public class DasuDao {
	
	@Id
	@Column(name = "dasu_id")
	private String id;
	
	/**
	 * The supervisor that runs this DASU 
	 */
	@ManyToOne
    @JoinColumn(name = "supervisor_id", foreignKey = @ForeignKey(name = "supervisor_id")
    )
    private SupervisorDao supervisor;
	
	/**
	 * The log level
	 * 
	 * A DASU inherits the supervisor log level if undefined in the CDB.
	 */
	@Enumerated(EnumType.STRING)
	@Basic(optional=true)
	private LogLevelDao logLevel;
	
	/**
	 * This one-to-many annotation matches with the many-to-one
	 * annotation in the {@link AsceDao} 
	 */
	@OneToMany(mappedBy = "dasu", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AsceDao> asces = new HashSet<>();
	
	public DasuDao() {}
	
	public LogLevelDao getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(LogLevelDao logLevel) {
		this.logLevel = logLevel;
	}

	public void addAsce(AsceDao asce) {
		Objects.requireNonNull(asce,"Cannot add a null ASCE to a DASU");
		asces.add(asce);
		asce.setDasu(this);
	}
	
	public void removeAsce(AsceDao asce) {
		Objects.requireNonNull(asce,"Cannot remove a null ASCE from a DASU");
		asces.remove(asce);
		asce.setDasu(null); // This won't work
	}

	public String getId() {
		return id;
	}

	public SupervisorDao getSupervisor() {
		return supervisor;
	}

	public void setSupervisor(SupervisorDao supervisor) {
		this.supervisor = supervisor;
	}

	public void setId(String id) {
		Objects.requireNonNull(id,"The DASU ID can't be null");
		String iden = id.trim();
		if (iden.isEmpty()) {
			throw new IllegalArgumentException("The DASU ID can't be an empty string");
		}
		this.id = iden;
	}
	
	public Set<AsceDao> getAsces() {
		return asces;
	}
	
	/**
	 * toString() prints a human readable version of the DASU
	 * where linked objects (like ASCES) are represented by their
	 * IDs only.
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("DASU=[ID=");
		ret.append(id);
		ret.append(", logLevel=");
		Optional.ofNullable(logLevel).ifPresent(x -> ret.append(x.toString()));
		ret.append(", Supervisor=");
		Optional.ofNullable(supervisor).ifPresent(x -> ret.append(x.getId()));
		ret.append(", ASCEs={");
		for (AsceDao asce: getAsces()) {
			ret.append(" ");
			ret.append(asce.getId());
		}
		ret.append("}]");
		return ret.toString();
	}
	
	public Set<String> getAscesIDs() {
		return asces.stream().map(x -> x.getId()).collect(Collectors.toSet());
	}

	/**
	 * <code>hashCode</code> is based on unique the ID only.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/**
	 * <code>equals</code> check the equality of the members of this object
	 * against the one passed in the command line but the checking of included
	 * ASCEs and SUPERVISOR is limited to their IDs.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {			
			return true;		
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		DasuDao other = (DasuDao) obj;
		if (asces == null) {
			if (other.asces != null)
				return false;
		} else if (!getAscesIDs().equals(other.getAscesIDs()))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (logLevel != other.logLevel)
			return false;
		if (supervisor == null) {
			if (other.supervisor != null)
				return false;
		} else if (!supervisor.getId().equals(other.supervisor.getId()))
			return false;
		return true;
	}
}
