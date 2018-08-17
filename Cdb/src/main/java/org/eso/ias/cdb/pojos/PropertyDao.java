package org.eso.ias.cdb.pojos;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * The property in the form of &lt;name, value&gt;
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
	
	/**
	 *  The name of the property
	 */
	private String name;
	
	/** 
	 * The value of the property
	 */
	private String value;
	
	/**
	 * Empty constructor
	 */
	public PropertyDao() {}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		Objects.requireNonNull(name,"The property name can't be null");
		String v=name.trim();
		if (v.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty property name");
		}
		this.name=v;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		Objects.requireNonNull(value,"The property name can't be null");
		String v=value.trim();
		if (v.isEmpty()) {
			throw new IllegalArgumentException("Invalid empty property value");
		}
		this.value=v;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}
		if (object==null || !(object instanceof PropertyDao)) {
			return false;
		}
		PropertyDao other = (PropertyDao)object;
		return Objects.equals(name,other.getName()) && 
				Objects.equals(value,other.getValue());
	}
	
	 @Override
	 public int hashCode() {
		 return Objects.hash(getName(),getValue());
	 }
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("Property=[");
		ret.append(getName());
		ret.append(',');
		ret.append(getValue());
		ret.append(']');
		return ret.toString();
	}
}
