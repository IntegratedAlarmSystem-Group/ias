package org.eso.ias.cdb.pojos;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * The Supervisor is the container of DASUs
 * needed to run more then one DASU in the same java process.
 * 
 * If we do not want this feature we can get rid of the Supervisor
 * by move its properties into the DASU pojo.
 *  
 * @author acaproni
 */
@Entity(name = "SUPERVISOR")
public class SupervisorDao {
	@Id
	private String id;
	
	/**
	 * The host where the Supervisor runs
	 */
	@Basic(optional=false)
	private String host;
	
	/**
	 * The log level
	 */
	@Enumerated(EnumType.STRING)
	private LogLevelDao logLevel;
	
	/**
	 * This one-to-many annotation matches with the many-to-one
	 * annotation in the {@link DasuDao} 
	 */
	@OneToMany(mappedBy = "supervisor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DasuDao> dasus = new ArrayList<DasuDao>();
}
