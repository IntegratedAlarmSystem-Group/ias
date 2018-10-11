package org.eso.ias.cdb.pojos;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * The pojo for the ASCE
 *
 * @author acaproni
 *
 */
@Entity
@Table(name = "ASCE")
public class AsceDao {

	@Id
	@Column(name = "asce_id")
	private String id;

	/**
	 * The DASU that runs this ASCE
	 */
	@ManyToOne
    @JoinColumn(name = "dasu_id", foreignKey = @ForeignKey(name = "dasu_id"))
    private DasuDao dasu;

	/**
	 * The transfer function
	 */
	@ManyToOne
	@JoinColumn(name = "transf_fun_id", foreignKey = @ForeignKey(name = "transf_fun_id"))
	private TransferFunctionDao transferFunction;

	/**
	 * The IASIOs in input to the ASCE.
	 *
	 * ManyToMany: the ASCE can have multiple IASIOs in input and the same IASIO can be
	 * the input of many ASCEs
	 */
	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(name= "ASCE_IASIO",
	joinColumns = @JoinColumn(name="asce_id"),
	inverseJoinColumns = @JoinColumn(name = "io_id"))
    private Set<IasioDao> inputs = new HashSet<>();

	/**
	 * The output generated by the ASCE applying the TF to the inputs
	 */
	@OneToOne
    @JoinColumn(name = "OUTPUT_ID")
	private IasioDao output;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinTable(name= "ASCE_PROPERTY",
		joinColumns = @JoinColumn(name="asce_id"),
		inverseJoinColumns = @JoinColumn(name = "props_id"))
	private Set<PropertyDao> props = new HashSet<>();

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name= "ASCE_TEMPL_IASIO",
			joinColumns = @JoinColumn(name="asce_id"),
			inverseJoinColumns = @JoinColumn(name = "templated_input_id"))
	private Set<TemplateInstanceIasioDao> templatedInstanceInputs = new HashSet<>();

	/**
	 * The ID of the template for implementing replication
	 */
	@Basic(optional=true)
	@Column(name = "template_id")
	private String templateId;

	public AsceDao() {}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		Objects.requireNonNull(id,"The ASCE ID can't be null");
		String iden = id.trim();
		if (iden.isEmpty()) {
			throw new IllegalArgumentException("The ASCE ID can't be an empty string");
		}
		this.id = iden;
	}

	public TransferFunctionDao getTransferFunction() {
		return transferFunction;
	}

	public void setTransferFunction(TransferFunctionDao transferFunction) {
		Objects.requireNonNull(transferFunction,"The TF of a ASCE can't be null");
		this.transferFunction = transferFunction;
	}

	public IasioDao getOutput() {
		return output;
	}

	public Collection<IasioDao> getInputs() {
		return inputs;
	}

	/**
	 * Add the passed input
	 *
	 * @param io The IASIO to put in the map
	 * @param replace If <code>true</code> replaces the old IASIO with
	 *                this one, if already present in the map
	 * @return <code>true</code> if the passed IASIO replace an object in the inputs list,
	 *         <code>false</code> otherwise
	 */
	public boolean addInput(IasioDao io, boolean replace) {
		Objects.requireNonNull(io, "Can't add a null IASIO");

		if (!inputs.contains(io)) {
			return inputs.add(io);
		} else {
			if (!replace) {
				return false;
			}
			return inputs.add(io);
		}
	}

    /**
     * Add the passed templated input
     *
     * @param tii The not <code>null</code> templated input to add
     * @param replace If <code>true</code> replaces the old templated input with
	 *                this one, if already present in the map
	 * @return <code>true</code> if the passed templated input has been aded,
	 *         <code>false</code> otherwise
     */
	public boolean addTemplatedInstanceInput(TemplateInstanceIasioDao tii, boolean replace) {
	    Objects.requireNonNull(tii);

	    if (!templatedInstanceInputs.contains(tii)) {
	        return templatedInstanceInputs.add(tii);
        } else {
	        if (!replace) {
				return false;
			}
			return templatedInstanceInputs.add(tii);
        }
    }

	public void setOutput(IasioDao output) {
		Objects.requireNonNull(output,"The output of a ASCE can't be null");
		this.output = output;
	}

	public Set<PropertyDao> getProps() {
		return props;
	}

	public void setDasu(DasuDao dasu) {
		this.dasu=dasu;
	}

	public DasuDao getDasu() {
		return dasu;
	}

	/**
	 * toString() prints a human readable version of the ASCE
	 * where linked objects (like DASU, IASIOS..) are represented by their
	 * IDs only.
	 */
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("ASCE=[ID=");
		ret.append(id);
		ret.append(", Output=");
		Optional.ofNullable(output).ifPresent(x -> ret.append(x));
		ret.append(", Inputs={");
		for (IasioDao iasio: getInputs()) {
			ret.append(' ');
			ret.append(iasio.getId());
		}
		ret.append("}, TF class={ className");
		ret.append(transferFunction.getClassName());
		ret.append(", implLang=");
		ret.append(transferFunction.getImplLang());
		ret.append("}, DASU=");
		Optional.ofNullable(dasu).ifPresent(x -> ret.append(x.getId()));
		ret.append(", Props={");
		for (PropertyDao prop: props) {
			ret.append(' ');
			ret.append(prop.toString());
		}
		ret.append("}");
		if (templateId!=null) {
			ret.append(", template id=\"");
			ret.append(templateId);
			ret.append('"');
		}
		if (templatedInstanceInputs!=null && !templatedInstanceInputs.isEmpty()) {
            ret.append(", templated inputs={");
            templatedInstanceInputs.forEach( ti -> {
                ret.append(' ');
                ret.append(ti.toString());
            });
        }
		ret.append("}]");
		return ret.toString();
	}

	public Set<String> getIasiosIDs() {
		return inputs.stream().map(x -> x.getId()).collect(Collectors.toSet());
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

    /**
     * Getter
     *
     * @return
     */
    public Set<TemplateInstanceIasioDao> getTemplatedInstanceInputs() {
        return templatedInstanceInputs;
    }

    /**
     * Setter
     *
     * @param templateInstances
     */
    public void setTemplatedInstanceInputs(Set<TemplateInstanceIasioDao> templateInstances) {
        if (templateInstances==null) {
            templatedInstanceInputs=new HashSet<TemplateInstanceIasioDao>();
        } else {
            templatedInstanceInputs=templateInstances;
        }
    }

	/**
	 * <code>hashCode</code> is based on unique the ID only.
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(id,templateId);
	}

	/**
	 * <code>equals</code> check the equality of the members of this object
	 * against the one passed in the command line but the checking of included
	 * DASU, and IASIOs is limited to their IDs.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AsceDao other = (AsceDao) obj;
		if (dasu == null) {
			if (other.dasu != null)
				return false;
		} else if (!dasu.getId().equals(other.dasu.getId()))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (inputs == null) {
			if (other.inputs != null)
				return false;
		} else if (!getIasiosIDs().equals(other.getIasiosIDs()))
			return false;
		if (output == null) {
			if (other.output != null)
				return false;
		} else if (!output.getId().equals(other.output.getId()))
			return false;
		if (props == null) {
			if (other.props != null)
				return false;
		} else if (!props.equals(other.props))
			return false;
		if (transferFunction == null) {
			if (other.transferFunction != null)
				return false;
		} else if (!transferFunction.equals(other.transferFunction))
			return false;
		if (templateId == null) {
			if (other.templateId != null)
				return false;
		} else if (!getTemplateId().equals(other.getTemplateId()))
			return false;
		if (templatedInstanceInputs == null) {
			if (other.templatedInstanceInputs != null)
				return false;
		} else if (!getTemplatedInstanceInputs().equals(other.getTemplatedInstanceInputs()))
			return false;
		return true;
	}

}
