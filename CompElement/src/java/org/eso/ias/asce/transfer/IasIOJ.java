package org.eso.ias.asce.transfer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.InOut;
import org.eso.ias.types.OperationalMode;

import scala.Predef;
import scala.Tuple2;
import scala.Option;
import scala.Some;
import scala.collection.JavaConverters;

/**
 * The java countr part of the scala IasIO, that is
 * the view of an InOut for the TF
 * 
 * IasIOJ exposes only the InOut's methods that can be
 * invoked by the TF hiding the methods meant for the
 * internals of the IAS.
 * 
 * The IasIo reduces the risk of errors from misuse from the 
 * TF and simplify the API hiding scala details. 
 * 
 * IasIo is immutable.
 * 
 * @author acaproni
 *
 */
public class IasIOJ<T> {
	
	/**
	 * The InOut to delegate
	 * 
	 * The visibility is limited to the package to avoi user implementation of the TF
	 * access internals of the IAS
	 */
	final InOut<T> inOut;

	/**
	 * Constructor
	 * 
	 * @param inOut The InOut to delegate
	 */
	public IasIOJ(InOut<T> inOut) {
		Objects.requireNonNull(inOut);
		this.inOut=inOut;
	}
	
	/**
	   * Update the mode of the monitor point
	   * 
	   * @param newMode: The new mode of the monitor point
	   */
	  public IasIOJ<T> updateMode(OperationalMode newMode) {
		  return new IasIOJ<T> (inOut.updateMode(newMode));
	  }
	  
	  /**
	   * Update the value of a IASIO
	   * 
	   * @param newValue: The new value of the IASIO
	   * @return A new InOut with updated value
	   */
	  public IasIOJ<T> updateValue(T newValue) {
		  Objects.requireNonNull(newValue);
	      Some<? super T> newValOpt = new Some<>(newValue);
	      return new IasIOJ<T>(inOut.updateValue(newValOpt));
	  }
	  
	  /**
	   * Set the validity constraints to the passed set of IDs of inputs.
	   * 
	   * The passed set contains the IDs of the inputs that the core must be consider
	   * when evaluating the validity of the output.
	   * The core returns an error if at least one of the ID is not 
	   * an input to the ASCE where the TF runs: in this case a message 
	   * is logged and the TF will not be run again.
	   * 
	   * To remove the constraints, the passed set must be empty
	   * 
	   * @param the constraint the constraints (can be null)
	   */
	  public IasIOJ<T> setValidityConstraint(Set<String> constraint) {
		  Option<scala.collection.immutable.Set<String>> scalaSet = Option.apply(null); // None
		  if (constraint!=null && !constraint.isEmpty()) {
			  scala.collection.mutable.Set<String> scalaMutableSet =  JavaConverters.asScalaSet(constraint);
			  scala.collection.immutable.Set<String> scalaImmutableSet =scalaMutableSet.toSet();
			  scalaSet = new Some<scala.collection.immutable.Set<String>>(scalaImmutableSet);
		  }
		  return new IasIOJ<T>(inOut.setValidityConstraint(scalaSet));
	  }
	  
	  /**
	   * Return a new IasIO with the passed additional properties.
	   * 
	   * @param The additional properties
	   * @return a new IasIOJ with the passed additional properties
	   */
    public IasIOJ<T> updateProps(Map<String, String> additionalProps) {
    	if (additionalProps==null) {
    		additionalProps = new HashMap<String, String>();
    	}
    	scala.collection.immutable.Map<String,String> immutableMap = 
    			JavaConverters.mapAsScalaMapConverter(additionalProps).asScala().toMap(
    		      Predef.<Tuple2<String, String>>conforms()
    		    );
    	
    	return new IasIOJ<T>(inOut.updateProps(immutableMap));
    }
    
    /**
     * 
     * @return the type of the monitor point
     */
    public IASTypes getType() {
    	return inOut.iasType();
    }
    
    /**
     * 
     * @return the identifier of the monitor point
     */
    public String getId() {
    	return inOut.id().id();
    }
    
    /**
     * 
     * @return the full running identifier of the monitor point
     */
    public String getFullrunningId() {
    	return inOut.id().fullRunningID();
    }
    
    /**
     * Note that a monitor point can be produced by a DASU or by a plugin
     * so only one between the plugin production timestamp and the
     * DASU production timestamp is defined.
     * 
     * @return The point in time when this monitor point has been produced by the DASU
     */
    public Optional<Long> dasuProductionTStamp() {
    	return Optional.ofNullable(inOut.dasuProductionTStamp().getOrElse(null));
    }
    
    /**
     * Note that a monitor point can be produced by a DASU or by a plugin
     * so only one between the plugin production timestamp and the
     * DASU production timestamp is defined.
     *  
     * @return The point in time when this monitor point has been produced by the plugin
     */
    public Optional<Long> pluginProductionTStamp() {
    	return Optional.ofNullable(inOut.pluginProductionTStamp().getOrElse(null));
    }
    
    /**
     * The properties of the monitor point 
     * 
     * @return the (unmodifiable) properties 
     */
    public Map<String,String> getProps() {
    	Option<scala.collection.immutable.Map<String,String>> inOutPropsOpt = inOut.props();
    	Map<String, String> javaProps;
    	if (inOutPropsOpt.isEmpty()) {
    		javaProps = new HashMap<String, String>();
    	} else {
    		javaProps =  JavaConverters.mapAsJavaMap(inOut.props().get());
    	}
    	return Collections.unmodifiableMap(javaProps);
    }
    
    /**
     * 
     * @return the actual value of the monitor point (can be <code>null</code>)
     */
    public Optional<T> getValue() {
    	if (inOut.value().isDefined()) {
    		T obj = (T)inOut.value().get();
    		return Optional.of(obj);
    	} else {
    		return Optional.empty();
    	}
    }
    
    /**
     * 
     * @return the operational mode
     */
    public OperationalMode getMode() {
    	return inOut.mode();
    }
    
    /**
     * The validity without taking times into account.
     * 
     * @return the validity
     */
    public IasValidity getValidity() {
    	return inOut.getValidity().iasValidity();
    }
    
    /** 
     * The validity of an input taking times into account
     * 
     *  @return the validity by tim
     */
    public IasValidity validityOfInputByTime(long threshold) {
    	return inOut.getValidityOfInputByTime(threshold).iasValidity();
    }
    
    /**
     * 
     * @return the validity constraints
     */
    public Set<String> getValidityConstraints() {
    	if (inOut.validityConstraint().isEmpty()) {
    		return new HashSet<String>();
    	} else {
    		return JavaConverters.setAsJavaSet(inOut.validityConstraint().get());
    	}
    }

}
