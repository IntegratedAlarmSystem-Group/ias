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
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * The DAO to handle the definition of the transfer function.
 * 
 * This initial version suits for scala and java.
 * 
 * @author acaproni
 *
 */
@Entity
@Table(name = "TRANSFER_FUNC")
public class TransferFunctionDao {

	/**
	 * The name of the class to execute
	 */
	@Id
	@Column(name = "className_id")
	private String className;
	
	/**
	 * This one-to-many annotation matches with the many-to-one
	 * annotation in the {@link AsceDao} 
	 */
	@OneToMany(mappedBy = "transferFunction", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<AsceDao> asces = new HashSet<>();
	
	/**
	 * Empty constructor
	 */
	public TransferFunctionDao() {
	}

	/**
	 * COnstructor
	 * 
	 * @param className The name of the class
	 * @param implLang the implementation language
	 */
	public TransferFunctionDao(String className, TFLanguageDao implLang) {
		Objects.requireNonNull(className, "The name of class can't be null");
		Objects.requireNonNull(implLang, "The implementation language can't be null");
		if (className.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid name of class");
		}
		this.className=className.trim();
		this.implLang=implLang;
	}
	
	/**
	 * The programming language of the transfer function
	 */
	@Enumerated(EnumType.STRING)
	@Basic(optional=false)
	@Column(name = "implLang")
	private TFLanguageDao implLang;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public TFLanguageDao getImplLang() {
		return implLang;
	}

	public void setImplLang(TFLanguageDao implLang) {
		this.implLang = implLang;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((implLang == null) ? 0 : implLang.hashCode());
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
		TransferFunctionDao other = (TransferFunctionDao) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (implLang != other.implLang)
			return false;
		return true;
	}

	public Set<AsceDao> getAsces() {
		return asces;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder("TF=[className=");
		ret.append(className);
		ret.append(", ImplLang=");
		ret.append(implLang);
		ret.append("]");
		return ret.toString();
	}
}
