package org.eso.ias.cdb.pojos;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * The DAO with the DASU to be deployed 
 * in a Supervisor.
 * 
 * @author acaproni
 *
 */
@Entity
@Table(name = "DASUS_TO_DEPLOY")
public class DasuToDeployDao {
	
	@Id
	@SequenceGenerator(name="DASU_TO_DEPLOY_SEQ_GENERATOR", sequenceName="DASU_TO_DEPLOY_SEQ_GENERATOR", allocationSize=1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="DASU_TO_DEPLOY_SEQ_GENERATOR")
	@Column(name = "id")
    private Long id;
	
	/**
	 * The DASU that runs this ASCE 
	 */
	@ManyToOne
    @JoinColumn(name = "dasu_id", foreignKey = @ForeignKey(name = "dasu_id"))
    private DasuDao dasu;
	
	/**
	 * The template if it is a templated DASU
	 */
	@OneToOne
	@JoinColumn(name = "template_id", foreignKey = @ForeignKey(name = "template_id"))
	private TemplateDao template;
	
	/**
	 * The number of instance of the DASU to deploy.
	 * 
	 * This is set only if the DASU is templated.
	 */
	@Basic(optional=true)
	@Column(name = "instance")
	private Integer instance;
	
	/**
	 * The supervisor that runs this DASU 
	 */
	@ManyToOne
    @JoinColumn(name = "supervisor_id", foreignKey = @ForeignKey(name = "supervisor_id")
    )
    private SupervisorDao supervisor;
	
	/**
	 * Empty constructor
	 */
	public DasuToDeployDao() {}

	/**
	 * Constructor
	 * @param dasu
	 * @param template
	 * @param instance
	 */
	public DasuToDeployDao(DasuDao dasu, TemplateDao template, Integer instance) {
		super();
		Objects.requireNonNull(dasu, "The DASU can't be null");
		this.dasu = dasu;
		this.template = template;
		this.instance = instance;
	}

	public DasuDao getDasu() {
		return dasu;
	}

	public void setDasu(DasuDao dasu) {
		this.dasu = dasu;
	}

	public TemplateDao getTemplate() {
		return template;
	}

	public void setTemplate(TemplateDao template) {
		this.template = template;
	}

	public Integer getInstance() {
		return instance;
	}

	public void setInstance(Integer instance) {
		this.instance = instance;
	}

	public SupervisorDao getSupervisor() {
		return supervisor;
	}

	public void setSupervisor(SupervisorDao supervisor) {
		this.supervisor = supervisor;
	}

	@Override
	public String toString() {
		return "DasuToDeployDao [id=" + id + ", dasu=" + dasu + ", template=" + template + ", instance=" + instance
				+ ", supervisor=" + supervisor + "]";
	}
}
