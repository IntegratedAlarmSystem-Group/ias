package org.eso.ias.types

import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Optional, TimeZone}

import scala.collection.JavaConverters

/**
 * A  <code>InOut</code> holds the value of an input or output 
 * of the IAS.
 * Objects of this type constitutes both the input of ASCEs and the output 
 * they produce. They are supposed to live into a ASCE only: their representation
 * in the BSDB is the IASValue[_].
 * 
 * The type of a InOut can be a double, an integer, an
 * array of integers and many other customized types.
 * 
 * The actual value is an Option because there is no
 * value associated before it comes for example from the HW. 
 * Nevertheless the <code>InOut</code> exists.
 * 
 * If the InOut is the input of a ASCE, it has the validity received from the IASValue
 * in the fromIasValueValidity. Otherwise the validity depends on the validity of the
 * inputs to the ASCE and is stored in fromInputsValidity.
 * At any time, only one Option between fromIasValueValidity and fromInputsValidity
 * must be defined: this invariant, can also used to recognize if a InoOut is an output
 * of the ACSE.
  *
  * @see isOutput()
 * 
 * A IASIO can only be produced by a plugin or by a DASU i.e.
 * only one between pluginProductionTStamp and dasuProductionTStamp
 * can be defined (another invariant)
 * 
 * A property of the InOut contains the list of the dependent monitor points.
 * Only an output can have dependent monitor points i.e. the InOut in input to the
 * DASu that are needed to produce the ouput.
 * The InOut in input have an empty set of dependant monitor points.
 * 
 * <code>InOut</code> is immutable.
 * @param value                             : the value of this InOut (can be empty)
 * @param id                                 : The unique ID of the monitor point
 * @param mode                               : The operational mode
 * @param readFromMonSysTStamp: The point in time when the value has been read from the
 *                              monitored system (set by the plugin only)
 * @param fromIasValueValidity               : The validity received from the BSDB (i.e. from a IASValue)
 *                                           It is None if and only if the value is generated by ASCE
 *                                           and in that case fromInputsValidity is defined
 * @param fromInputsValidity                 the validity inherited by the inputs
 *                                           It is defined only for the outputs of an ASCE
 *                                           and in that case  fromIasValueValidity must be None
 * @param validityConstraint allows the ASCEs and the DASUs to restrict the evaluation
 *                           of the validity to a subset of inputs
 *                           As the IASValue read from the BSDB have no inputs,
 *                           validityConstraint is defined only when fromInputsValidity
 *                           is defined but not the other way around
 * @param iasType: is the IAS type of this InOut
 * @param pluginProductionTStamp The point in time when the plugin produced this value
 * @param sentToConverterTStamp The point in time when the plugin sent the value to the converter
 * @param receivedFromPluginTStamp The point in time when the converter received the value from the plugin
 * @param convertedProductionTStamp The point in time when the converter generated
 *                                  the value from the data structure rceived by the plugin
 * @param sentToBsdbTStamp The point in time when the value has been sent to the BSDB
 * @param readFromBsdbTStamp                 The point in time when the value has been read from the BSDB
 * @param dasuProductionTStamp               The point in time when the value has been generated by the DASU
 * @param idsOfDependants                    the identifiers of the dependent monitor points i.e.
 *                                           the identifier of the inputs if this InOut represents a output
 *                                           empty otherwise
 * @param props                              additional properties if any, otherwise empty
  * @see IASType
  * @author acaproni
 */
case class InOut[A](
    value: Option[_ >: A],
    id: Identifier,
    mode: OperationalMode,
    fromIasValueValidity: Option[Validity],
    fromInputsValidity: Option[Validity],
    validityConstraint: Option[Set[String]],
    iasType: IASTypes,
    readFromMonSysTStamp: Option[Long],
    pluginProductionTStamp: Option[Long],
	  sentToConverterTStamp: Option[Long],
	  receivedFromPluginTStamp: Option[Long],
	  convertedProductionTStamp: Option[Long],
	  sentToBsdbTStamp: Option[Long],
	  readFromBsdbTStamp: Option[Long],
	  dasuProductionTStamp: Option[Long],
	  idsOfDependants: Option[Set[Identifier]],
	  props: Option[Map[String,String]]) {
  require(Option[Identifier](id).isDefined,"The identifier must be defined")
  require(Option[IASTypes](iasType).isDefined,"The type must be defined")
  require(Option(idsOfDependants).isDefined,"Invalid list of dep. identifiers")
  
  // Check that one and only one validity (from inputs or from IASValue)
  // is defined
  require(
      fromIasValueValidity.size+fromInputsValidity.size==1,
      "Inconsistent validity")
      
  // Check that when validityConstraint is defined, also fromInputsValidity
  // is defined
  require(validityConstraint.forall( c => {
    !c.isEmpty && isOutput
  }), "Inconsistent validity constraint")
      
  // Check that no more then one between the plugin and the DASU production
  // timestamps is defined
  require(
      pluginProductionTStamp.size+dasuProductionTStamp.size<=1,
      "Inconsistent production timestamps")
  
  value.foreach(v => assert(InOut.checkType(v,iasType),"Type mismatch: ["+v+"] is not "+iasType))
  
  override def toString(): String = {
    val ret = new StringBuilder("Monitor point [")
    ret.append(id.toString())
    ret.append("] of type ")
    ret.append(iasType)
    ret.append(", mode=")
    ret.append(mode.toString())
    fromIasValueValidity.foreach(v => {
      ret.append(", from BSDB validity=")
      ret.append(v.toString())
    })
    fromInputsValidity.foreach(v => {
      ret.append(", from inputs validity=")
      ret.append(v.toString())
    })
    ret.append(", ")
    if (value.isEmpty) {
      ret.append("No value")
    } else {
       ret.append("Value: "+value.get.toString())
    }

    readFromMonSysTStamp.foreach(t => { ret.append(", readFromMonSysTStamp="); ret.append(t); })
    pluginProductionTStamp.foreach(t => { ret.append(", pluginProductionTStamp="); ret.append(t); })
	  sentToConverterTStamp.foreach(t => { ret.append(", sentToConverterTStamp="); ret.append(t); })
	  receivedFromPluginTStamp.foreach(t => { ret.append(", receivedFromPluginTStamp="); ret.append(t); })
	  convertedProductionTStamp.foreach(t => { ret.append(", convertedProductionTStamp="); ret.append(t); })
	  sentToBsdbTStamp.foreach(t => { ret.append(", sentToBsdbTStamp="); ret.append(t); })
	  readFromBsdbTStamp.foreach(t => { ret.append(", readFromBsdbTStamp="); ret.append(t); })
	  dasuProductionTStamp.foreach(t => { ret.append(", dasuProductionTStamp="); ret.append(t); })
	  
	  idsOfDependants.foreach( ids => {
	    ret.append(", Ids of dependants=[")
	    val listOfIds = ids.map(_.id).toList.sorted
	    ret.append(listOfIds.mkString(", "))
	    ret.append(']')  
	  })
	  
	  props.foreach( pMap => {
	     ret.append(", properties=[")
	     val keys = pMap.keys.toList.sorted
	     keys.foreach(key => {
	       ret.append('(')
	       ret.append(key)
	       ret.append(',')
	       ret.append(pMap(key))
	       ret.append(')')
	     })
	     ret.append(']') 
	  })
	  
    ret.toString()
  }
  
  /**
   * Update the mode of the monitor point
   * 
   * @param newMode: The new mode of the monitor point
   */
  def updateMode(newMode: OperationalMode): InOut[A] = {
    require(Option(newMode).isDefined)
    this.copy(mode=newMode)
  }
  
  /**
   * Update the value of a IASIO
   * 
   * @param newValue: The new value of the IASIO
   * @return A new InOut with updated value
   */
  def updateValue(newValue: Some[_ >: A]): InOut[A] = {
    assert(InOut.checkType(newValue.get,iasType))
    
    this.copy(value=newValue)
  }
  
  /**
   * Update the value and validity of the monitor point.
   * 
   * Which validity to updated between fromIasValueValidity and fromInputsValidity
   * depends if the InOut is an input or a output of a ASCE.
   * 
   * @param newValue: The new value of the monitor point
   * @param newValidity the new validity (either fromIasValueValidity or fromInputsValidity)
   * @return A new InOut with updated value and validity
   */
  def updateValueValidity(newValue: Some[_ >: A], newValidity: Some[Validity]): InOut[A] = {
    assert(InOut.checkType(newValue.get,iasType))
    if (isOutput()) {
      this.copy(value=newValue,fromInputsValidity=newValidity)
    } else {
      this.copy(value=newValue,fromIasValueValidity=newValidity)
    }
  }
  
  /**
   * Update the validity received from a IASValue
   */
  def updateFromIasValueValidity(validity: Validity):InOut[A] = {
    val validityOpt = Option(validity)
    require(validityOpt.isDefined)
    assert(!isOutput() && fromIasValueValidity.isDefined, "Cannot update the IASValue validity of an output")
    this.copy(fromIasValueValidity=validityOpt)
  }
  
  /**
   * Update the validity Inherited from the inputs
   */
  def updateFromIinputsValidity(validity: Validity):InOut[A] = {
    val validityOpt = Option(validity)
    require(validityOpt.isDefined)
    assert(isOutput() && fromInputsValidity.isDefined, "Cannot update the validities of inputs of an input")
    this.copy(fromInputsValidity=validityOpt)
  }
  
  /**
   * Set the validity constraints to the passed set of IDs of inputs
   * 
   * @param constraint the constraints
   */
  def setValidityConstraint(constraint: Option[Set[String]]):InOut[A] = {
    require(Option(constraint).isDefined,"Invalid validity constraint")
    require(isOutput(),"Validity constraint can be set only for output IASIO")
    
    // Replaces an empty Set with none
    val reviewedConstraints = constraint.flatMap( c => if (c.isEmpty) None else Some(c))
    
    this.copy(validityConstraint=reviewedConstraints)
  }
  
  /**
   * @return true if this IASIO is the generated by the ASCE,
   *         false otherwise (i.e. the input of the ASCE)
   */
  def isOutput() = fromIasValueValidity.isEmpty
  
  /**
   * The validity that comes either from the IASValue (input of a ASCE)
   * or inherited from the inputs (output of the ASCE)
   * 
   * The validity returned by this method does not take into account 
   * the actual time against the timestamps of the IASIO
   * 
   * @return the validity 
   */
  def getValidity: Validity = {
    assert(
      fromIasValueValidity.isDefined && fromInputsValidity.isEmpty ||
      fromIasValueValidity.isEmpty && fromInputsValidity.isDefined,
      "Inconsistent validity")
      
      fromIasValueValidity.getOrElse(fromInputsValidity.get)
  }
  
  /**
   * The validity of a InOut in input, taking times into account.
   * 
   * This validity takes into account only the time of the update of
   * this monitor point. The validity of the output of a monitor
   * point generated by the TF of an ASCE, depends also on the refresh 
   * time of its inputs that for this reason cannot be evaluated at this level.
   * 
   * The monitor point is valid if it has been updated before 
   * the validityTimeFrame elapses.
   * 
   *  
   * 
   * @param validityTimeFrame the time (msecs) to consider the value valid
   *                          it is composed of the autoSendTimeInterval+tolerance 
   * @return the validity taking times into account
   */
  def getValidityOfInputByTime(validityTimeFrame: Long): Validity = {
    require(validityTimeFrame>0, "Invalid time frame")
    require(!isOutput(),"The validty cannot by evaluated for output IASIO")
    
    assert(
      dasuProductionTStamp.isDefined && pluginProductionTStamp.isEmpty ||
      dasuProductionTStamp.isEmpty && pluginProductionTStamp.isDefined,
      "Inconsistent production TStamps")
    
    val thresholdTStamp = System.currentTimeMillis() - validityTimeFrame
    val iasioTstamp: Long = dasuProductionTStamp.getOrElse(pluginProductionTStamp.get)

    assert(iasioTstamp<=thresholdTStamp+validityTimeFrame)
    
    val validityByTime = if (iasioTstamp<thresholdTStamp) {
        Validity(IasValidity.UNRELIABLE)
    } else {
        Validity(IasValidity.RELIABLE)
    }

    val  date: java.util.Date = new java.util.Date(iasioTstamp)
    val formatter: DateFormat = new SimpleDateFormat("HH:mm:ss.SSS")
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
    val iasioTSTampDateFormatted = formatter.format(date)
    val thresholdDateFormatted = formatter.format(new java.util.Date(thresholdTStamp))

    println(id.id+
      ": iasioTstamp="+iasioTSTampDateFormatted+
      " threshold="+thresholdDateFormatted+
      " (diff="+(thresholdTStamp-iasioTstamp)+
      ") validityByTime="+validityByTime.iasValidity)

    Validity.minValidity(Set(validityByTime,getValidity))
  }
  
  /**
   * Updates the IDs of the dependents
   * 
   * @param idsOfDeps the identifiers of the dependent monitor points
   * @return a new InOut with the passed IDs of the dependents
   */
  def updateDependentsIds(idsOfDeps: Set[Identifier]): InOut[A] = {
    val idsOfDepsOpt = Option(idsOfDeps)
    require(idsOfDepsOpt.isDefined,"Cannot update the list of dependents with an empty set of identifiers")
    if (idsOfDepsOpt.get.isEmpty) {
      this.copy(idsOfDependants=None)
    } else {
      this.copy(idsOfDependants=idsOfDepsOpt)
    }
  }
  
  /**
   * Return a new inOut with the passed additional properties
   * 
   * @param The additional properties
   * @return a new inOut with the passed additional properties
   */
  def updateProps(additionalProps: Map[String,String]): InOut[A] = {
    val propsOpt = Option(additionalProps)
    if (propsOpt.get.isEmpty) {
      this.copy(props = None)
    } else {
      this.copy(props = propsOpt)
    }
  }
  
  /**
   * Update the value of this IASIO with the IASValue.
   * 
   * The validity received from the IASValue will be stored in fromIasValueValidity
   * if this INOut is an input of a ASCE or in fromInputsValidity if it the the output 
   * of a ASCE.
   */
  def update(iasValue: IASValue[_]): InOut[A] = {
    require(Option(iasValue).isDefined,"Cannot update from a undefined IASValue")
    require(Option(iasValue.value).isDefined,"Cannot update when the IASValue has no value")
    require(Option(iasValue.dependentsFullRuningIds).isDefined,"Cannot update when the IASValue has no dependent ids")
    assert(iasValue.id==this.id.id,"Identifier mismatch: received "+iasValue.id+", expected "+this.id.id)
    assert(iasValue.valueType==this.iasType)
    assert(InOut.checkType(iasValue.value,iasType))
    val validity = Some(Validity(iasValue.iasValidity))
    
    val depIds = if (iasValue.dependentsFullRuningIds.isPresent() && !iasValue.dependentsFullRuningIds.get.isEmpty()) {
      Some(Set.empty++JavaConverters.asScalaSet(iasValue.dependentsFullRuningIds.get).map(Identifier(_)))
    } else {
      None
    }
    
    val addProps = if (iasValue.props.isPresent() && !iasValue.props.get.isEmpty()) {
      Some(Map.empty++JavaConverters.mapAsScalaMap(iasValue.props.get))
    } else {
      None
    }
    
    new InOut(
        Some(iasValue.value),
        Identifier(iasValue.fullRunningId),
        iasValue.mode,
        if (isOutput()) None else validity,
        if (isOutput()) validity else None,
        None,
        iasValue.valueType,
        if (iasValue.readFromMonSysTStamp.isPresent) Some(iasValue.readFromMonSysTStamp.get()) else None,
        if (iasValue.pluginProductionTStamp.isPresent()) Some(iasValue.pluginProductionTStamp.get()) else None,
	      if (iasValue.sentToConverterTStamp.isPresent()) Some(iasValue.sentToConverterTStamp.get()) else None,
	      if (iasValue.receivedFromPluginTStamp.isPresent()) Some(iasValue.receivedFromPluginTStamp.get()) else None,
	      if (iasValue.convertedProductionTStamp.isPresent()) Some(iasValue.convertedProductionTStamp.get()) else None,
	      if (iasValue.sentToBsdbTStamp.isPresent()) Some(iasValue.sentToBsdbTStamp.get()) else None,
	      if (iasValue.readFromBsdbTStamp.isPresent()) Some(iasValue.readFromBsdbTStamp.get()) else None,
	      if (iasValue.dasuProductionTStamp.isPresent()) Some(iasValue.dasuProductionTStamp.get()) else None,
	      depIds,
	      addProps)
  }
  
  def updateSentToBsdbTStamp(timestamp: Long): InOut[A] = {
    val newTimestamp = Option(timestamp)
    require(newTimestamp.isDefined)
    
    this.copy(sentToBsdbTStamp=newTimestamp)
  }
  
  def updateDasuProdTStamp(timestamp: Long): InOut[A] = {
    val newTimestamp = Option(timestamp)
    require(newTimestamp.isDefined)
    
    this.copy(dasuProductionTStamp=newTimestamp)
  }
  /**
   * Build and return the IASValue representation of this IASIO
   * 
   * @return The IASValue representation of this IASIO
   */
  def toIASValue(): IASValue[A] = {
    
    val ids = idsOfDependants.map( i => JavaConverters.setAsJavaSet(i.map(_.fullRunningID)))
    
    val p = props.map( p => JavaConverters.mapAsJavaMap(p))
    
    val theValue = if (value.isDefined) {
      value.get.asInstanceOf[A]
    } else {
      null
    }
    
    new IASValue[A](
        theValue.asInstanceOf[A],
			  mode,
			  getValidity.iasValidity,
			  id.fullRunningID,
			  iasType,
        Optional.ofNullable(if (readFromMonSysTStamp.isDefined) readFromMonSysTStamp.get else null),
			  Optional.ofNullable(if (pluginProductionTStamp.isDefined) pluginProductionTStamp.get else null),
  			Optional.ofNullable(if (sentToConverterTStamp.isDefined) sentToConverterTStamp.get else null),
  			Optional.ofNullable(if (receivedFromPluginTStamp.isDefined) receivedFromPluginTStamp.get else null),
  			Optional.ofNullable(if (convertedProductionTStamp.isDefined) convertedProductionTStamp.get else null),
  			Optional.ofNullable(if (sentToBsdbTStamp.isDefined) sentToBsdbTStamp.get else null),
  			Optional.ofNullable(if (readFromBsdbTStamp.isDefined) readFromBsdbTStamp.get else null),
  			Optional.ofNullable(if (dasuProductionTStamp.isDefined) dasuProductionTStamp.get else null),
  			Optional.ofNullable(if (idsOfDependants.isDefined) ids.get else null),
  			Optional.ofNullable(if (p.isDefined) p.get else null))

  }
  
  
}

/** 
 *  InOut companion object
 */
object InOut {
  
  /**
   * Check if the passed value is of the proper type
   * 
   * @param value: The value to check they type against the iasType
   * @param iasType: The IAS type
   */
  def checkType[T](value: T, iasType: IASTypes): Boolean = {
    if (value==None) true
    else iasType match {
      case IASTypes.LONG => value.isInstanceOf[Long]
      case IASTypes.INT => value.isInstanceOf[Int]
      case IASTypes.SHORT => value.isInstanceOf[Short]
      case IASTypes.BYTE => value.isInstanceOf[Byte]
      case IASTypes.DOUBLE => value.isInstanceOf[Double]
      case IASTypes.FLOAT => value.isInstanceOf[Float]
      case IASTypes.BOOLEAN =>value.isInstanceOf[Boolean]
      case IASTypes.CHAR => value.isInstanceOf[Char]
      case IASTypes.STRING => value.isInstanceOf[String]
      case IASTypes.ALARM =>value.isInstanceOf[Alarm]
      case _ => false
    }
  }
  
  /**
   * Build a InOut that is an output of a ASCE.
   * This InOut has the validity inherited from the IASValue initially set to INVALID
   * 
   * Such a IASIO is useful when it is expected but has not yet been sent
   * by the BSDB or a ASCE: we know that it exists but we do not know yet its 
   * initial value.
   * 
   * @param id the identifier
   * @param iasType the type of the value of the IASIO
   * @return a InOut initially empty
   */
  def asOutput[T](id: Identifier, iasType: IASTypes): InOut[T] = {
    new InOut[T](
        None,
        id,
        OperationalMode.UNKNOWN,
        None,
        Some(Validity(IasValidity.UNRELIABLE)),
        None, 
        iasType,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None)
  }
  
  /**
   * Build a InOut that is the input of a ASCE.
   * This InOut has the validity inherited from the validities of
   * the inputs of the ASCE  initially set to INVALID
   * 
   * Such a IASIO is useful when it is expected but has not yet been sent
   * by the BSDB or a ASCE: we know that it exists but we do not know yet its 
   * initial value.
   * 
   * @param id the identifier
   * @param iasType the type of the value of the IASIO
   * @return a InOut initially empty
   */
  def asInput[T](id: Identifier, iasType: IASTypes): InOut[T] = {
    new InOut[T](
        None,
        id,
        OperationalMode.UNKNOWN,
        Some(Validity(IasValidity.UNRELIABLE)),
        None,
        None,
        iasType,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None)
  }
}