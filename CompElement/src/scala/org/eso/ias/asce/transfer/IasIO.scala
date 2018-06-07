package org.eso.ias.asce.transfer

import org.eso.ias.types.IASTypes;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.InOut
import org.eso.ias.types.OperationalMode;

import scala.Option;
import org.eso.ias.types.Validity
import org.eso.ias.types.IasValidity

/**
 * The view of an InOut for the TF
 * 
 * IasIO exposes only the InOut's methods that can be
 * invoked by the TF hiding the methods meant for the
 * internals of the IAS.
 * 
 * The IasIo reduces the risk of errors from misuse from the 
 * TF and simplify the API
 * 
 * IasIo is immutable.
 * 
 * @param inOut The InOut to delegate
 */
class IasIO[T](private[transfer] val inOut: InOut[T]) {
  require(Option(inOut).isDefined)
  
  /** The IAS type of the monitor point */
  lazy val iasType: IASTypes = inOut.iasType
  
  /** The identifier of the monitor point */
  lazy val id: String = inOut.id.id
  
  /** The full running identifier of the monitor point */
  lazy val fullRunningId: String = inOut.id.fullRunningID
  
  /** 
   *  The validity
   *  
   *  This is the validity without taking times into account.
   */
  lazy val validity: IasValidity = inOut.getValidity.iasValidity
  
  /**
   * The point in time when this monitor point has been produced by the DASU
   * 
   * Note that a monitor point can be produced by a DASU or by a plugin:
   * one and only one between the plugin production timestamp and the
   * DASU production timestamp is defined.
   */
  lazy val dasuProductionTStamp: Option[Long] = inOut.dasuProductionTStamp
  
  /**
   * The point in time when this monitor point has been produced by the plugin
   * 
   * Note that a monitor point can be produced by a DASU or by a plugin:
   * one and only one between the plugin production timestamp and the
   * DASU production timestamp is defined.
   */
  lazy val pluginProductionTStamp: Option[Long] = inOut.pluginProductionTStamp
  
  /**
   * The additional properties
   */
  lazy val props: Map[String, String] = inOut.props.getOrElse(Map.empty)
  
  /** The actual value */
  val value: Option[T] = inOut.value.asInstanceOf[Option[T]]
  
  /** The operational mode */
  lazy val mode: OperationalMode = inOut.mode
  
  /** The validity constraints */
  lazy val validityConstraints: Option[Set[String]] = inOut.validityConstraint
  
  /** The validity of an input taking times into account */
  def validityOfInputByTime(threshold: Long): IasValidity = inOut.getValidityOfInputByTime(threshold).iasValidity
  
  /**
   * Update the mode of the monitor point
   * 
   * @param newMode: The new mode of the monitor point
   */
  def updateMode(newMode: OperationalMode): IasIO[T] = new IasIO[T](inOut.updateMode(newMode))
  
  /**
   * Update the value of a IASIO
   * 
   * @param newValue: The not null new value of the IASIO
   * @return A new InOut with updated value
   */
  def updateValue[B >: T](newValue: B): IasIO[T] = {
    val newValOpt = Option(newValue)
    require(newValOpt.isDefined,"The new value must be defined")
    new IasIO[T](inOut.updateValue(Some(newValue)))
  }
  
  /**
   * Set the validity constraints to the passed set of IDs of inputs.
   * 
   * The passed set contains the IDs of the inputs that the core must be consider
   * when evaluating the validity of the output.
   * The core returns an error if at least one of the ID is not 
   * an input to the ASCE when the TF runs: in this case a message 
   * is logged and the TF will not be run again.
   * 
   * To remove the constrans, the passed set must be empty
   * 
   * @param constraint the constraints
   */
  def setValidityConstraint(constraint: Option[Set[String]]):IasIO[T] = 
    
    
    new IasIO[T](inOut.setValidityConstraint(constraint))
  
  /**
   * Return a new IasIO with the passed additional properties.
   * 
   * @param The additional properties
   * @return a new inOut with the passed additional properties
   */
  def updateProps(additionalProps: Map[String,String]): IasIO[T] = 
    new IasIO[T](inOut.updateProps(additionalProps))
}