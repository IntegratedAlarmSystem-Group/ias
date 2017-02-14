package org.eso.ias.cdb.pojos;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * The property in the form of <name, value>
 * 
 * @author acaproni
 *
 */
@Entity
@Table(name = "PROPERTY")
public class PropertyDao {
	@Id
	@SequenceGenerator(name="PROP_SEQ_GENERATOR", sequenceName="PROP_SEQ_GENERATOR", allocationSize=1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="PROP_SEQ_GENERATOR")
	@Column(name = "id")
    private Long id;
	
	// The name of the property
	private String name;
	
	// The value of the property
	private String value;
	
	/**
	 * Empty constructor
	 */
	public PropertyDao() {}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name=name;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value=value;
	}
}
