package org.eso.ias.cdb.pojos;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class IasDao  implements Serializable {
	
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
}
