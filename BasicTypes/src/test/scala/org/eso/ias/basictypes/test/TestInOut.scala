package org.eso.ias.basictypes.test

import org.eso.ias.types.*
import org.eso.ias.types.IasValidity.*
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.JavaConverters
import scala.jdk.javaapi.CollectionConverters

/**
 * Test the LongMP
 *
 * @author acaproni
 */
class TestInOut extends AnyFlatSpec {
  val supervId = new Identifier("SupervId", IdentifierType.SUPERVISOR, None)
  val dasuId = new Identifier("DasuId", IdentifierType.DASU, supervId)
  val asceId = new Identifier("AsceId", IdentifierType.ASCE, Some(dasuId))

  // The ID of the alarms built in this test
  //
  // This test is all about the conversion
  val id = new Identifier("LongMPID", IdentifierType.IASIO, Some(asceId))

  behavior of "A IASIO"

  it must "have an ID" in {
    val mp: InOut[Long] = InOut.asInput(id, IASTypes.LONG)

    assert(!mp.value.isDefined)
    assert(mp.mode == OperationalMode.UNKNOWN)

    assert(mp.fromIasValueValidity.isDefined)
    assert(mp.fromInputsValidity.isEmpty)
  }

  it must "Have the same ID after changing other props" in {
    val mp: InOut[Long] = new InOut(
      Option.empty,
      id,
      OperationalMode.OPERATIONAL,
      Some(RELIABLE),
      None,
      None,
      IASTypes.LONG,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None)
    
    // Change the value of the previous MP
    val mp2 = mp.updateValue(Some(3L))
    assert(mp2.id == mp.id)
    assert(mp2.value.isDefined)
    assert(mp2.value.get == 3L)
    // Trivial check of the update of the timestamp 
    assert(mp2.productionTStamp.isDefined)
    assert(mp2.mode == OperationalMode.OPERATIONAL)
    assert(mp2.fromIasValueValidity.isDefined)
    assert(mp2.fromIasValueValidity.get == Validity(RELIABLE))

    // Change validity of the previous MP
    val mp3 = mp2.updateFromIasValueValidity(Validity(UNRELIABLE))
    assert(mp3.id == mp.id)
    assert(mp3.value.isDefined)
    assert(mp3.value.get == mp2.value.get)
    assert(mp3.mode == mp2.mode)
    assert(mp3.fromIasValueValidity.isDefined)
    assert(mp3.fromIasValueValidity.get == Validity(UNRELIABLE))

    // Change mode of the previous MP
    val mp4 = mp3.updateMode(OperationalMode.OPERATIONAL)
    assert(mp4.id == mp.id)
    assert(mp4.value.isDefined)
    assert(mp4.value.get == mp3.value.get)
    assert(mp4.mode == OperationalMode.OPERATIONAL)
    assert(mp4.fromIasValueValidity.isDefined)
    assert(mp4.fromIasValueValidity.get == mp3.fromIasValueValidity.get)
  }

  it must "allow to update the value" in {
    val mp: InOut[Long] = InOut.asOutput(id,  IASTypes.LONG)
    val mpUpdatedValue = mp.updateValue(Some(5L))
    assert(mpUpdatedValue.value.get == 5L, "The values differ")
  }

  it must "allow to update the validity inherited from the IASValue" in {
    val mpInput: InOut[Long] = InOut.asInput(id, IASTypes.LONG)
    assert(mpInput.fromIasValueValidity.isDefined)
    
    val mp2= mpInput.updateFromIasValueValidity(Validity(RELIABLE))
    assert(mp2.fromIasValueValidity.isDefined)
    assert(mp2.fromIasValueValidity.get == Validity(RELIABLE), "The validities differ")
  }
  
  it must "allow to update the validity inherited from the inputs" in {
    val mpOutput: InOut[Long] = InOut.asOutput(id, IASTypes.LONG)
    assert(mpOutput.fromInputsValidity.isDefined)

    val mp2 = mpOutput.updateFromInputsValidity(Validity(RELIABLE))
    assert(mp2.fromInputsValidity.isDefined)
    assert(mp2.fromInputsValidity.get == Validity(RELIABLE), "The validities differ")
  }

  it must "allow to update the mode" in {
    val mp: InOut[Long] = InOut.asInput(id, IASTypes.LONG)
    val mpUpdatedMode = mp.updateMode(OperationalMode.OPERATIONAL)
    assert(mpUpdatedMode.mode == OperationalMode.OPERATIONAL, "The modes differ")
  }

  it must "allow to update the value and validity at once" in {
    val mp: InOut[Long] = InOut.asOutput(id, IASTypes.LONG)
    val mpUpdated = mp.updateValueValidity(Some(15L), Some(Validity(RELIABLE)))
    assert(mpUpdated.value.get == 15L, "The values differ")
    assert(mpUpdated.fromIasValueValidity.isEmpty)
    assert(mpUpdated.fromInputsValidity.isDefined)
    assert(mpUpdated.fromInputsValidity.get == Validity(RELIABLE), "The validities differ")
  }

  it must "update the production timestamp" in {
    val mp = InOut.asInput(id,IASTypes.LONG)
    assert(mp.productionTStamp.isEmpty)
    val mp2=mp.updateProdTStamp(System.currentTimeMillis());
    assert(mp2.productionTStamp.isDefined)
  }

  it must "support all types" in {
    // Build a IASIO of all supported types and update the value checking
    // for possible mismatch
    val hioLong: InOut[Long] = InOut.asInput(id, IASTypes.LONG)
    val hioShort: InOut[Short] = InOut.asInput(id, IASTypes.SHORT)
    val hioInt: InOut[Int] = InOut.asInput(id, IASTypes.INT)
    val hioByte: InOut[Byte] = InOut.asInput(id, IASTypes.BYTE)
    val hioDouble: InOut[Double] = InOut.asInput(id, IASTypes.DOUBLE)
    val hioFloat: InOut[Float] = InOut.asInput(id, IASTypes.FLOAT)
    val hioBool: InOut[Boolean] = InOut.asInput(id, IASTypes.BOOLEAN)
    val hioChar: InOut[Char] = InOut.asInput(id, IASTypes.CHAR)
    val hioString: InOut[String] = InOut.asInput(id, IASTypes.STRING)
    val hioTStamp: InOut[Long] = InOut.asInput(id, IASTypes.TIMESTAMP)
    val hioArrayLongs: InOut[Long] = InOut.asInput(id, IASTypes.ARRAYOFLONGS)
    val hioArrayDoubles: InOut[Long] = InOut.asInput(id, IASTypes.ARRAYOFDOUBLES)
    val hioAlarm: InOut[Alarm] = InOut.asInput(id, IASTypes.ALARM)

    // Check if all the types has been instantiated
    val listOfHIOs = List(
      hioLong,
      hioShort,
      hioInt,
      hioByte,
      hioDouble,
      hioFloat,
      hioBool,
      hioChar,
      hioString,
      hioAlarm,
      hioTStamp,
      hioArrayDoubles,
      hioArrayLongs)
    assert(listOfHIOs.size == IASTypes.values().length)

    hioLong.updateValue(Some(-1L))
    hioShort.updateValue(Some(2.toShort))
    hioInt.updateValue(Some(13))
    hioByte.updateValue(Some(64.toByte))
    hioDouble.updateValue(Some(-1.9D))
    hioFloat.updateValue(Some(-1.3F))
    hioBool.updateValue(Some(false))
    hioChar.updateValue(Some('C'))
    hioString.updateValue(Some("Test"))
    hioTStamp.updateValue(Some(System.currentTimeMillis()))
    hioAlarm.updateValue(Some(Alarm.getInitialAlarmState))

  }

  it must "build and update from a passed IASValue" in {

    val monitoredSysId = new Identifier("MonSysId", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
    val pluginId = new Identifier("PluginId", IdentifierType.PLUGIN, Option(monitoredSysId))
    val converterId = new Identifier("ConverterId", IdentifierType.CONVERTER, Some(pluginId))
    val iasioId = new Identifier("IasioId", IdentifierType.IASIO, Option(converterId))

    // Build the IASIO from the passed IASValue
    val iasValue = IASValue.build(
        821L, 
        OperationalMode.INITIALIZATION, 
        RELIABLE, 
        iasioId.fullRunningID,
        IASTypes.LONG)
        
    val inOut = InOut.asInput(iasioId,IASTypes.LONG).update(iasValue)

    assert(inOut.iasType == iasValue.valueType)
    assert(inOut.value.isDefined)
    assert(inOut.value.get.asInstanceOf[Long] == iasValue.value.asInstanceOf[Long])
    assert(inOut.mode == iasValue.mode)
    
    assert(inOut.fromIasValueValidity.isDefined)
    assert(inOut.fromIasValueValidity.get.iasValidity==iasValue.iasValidity)

    // Update a IASIO with no value with a passed IASValue
    val iasio: InOut[?] = InOut.asOutput(iasioId, IASTypes.LONG)
    val newiasIo = iasio.update(iasValue)
    assert(newiasIo.iasType == iasValue.valueType)
    assert(newiasIo.value.isDefined)
    assert(newiasIo.value.get == iasValue.value)
    assert(newiasIo.mode == iasValue.mode)
    assert(newiasIo.fromIasValueValidity.isEmpty)
    assert(newiasIo.fromInputsValidity.isDefined)

    // Update with another value
    val iasValue2 = IASValue.build(
        113142L, 
        OperationalMode.OPERATIONAL, 
        UNRELIABLE, 
        iasioId.fullRunningID,
        IASTypes.LONG)
      
    val newiasIo2 = iasio.update(iasValue2)
    assert(newiasIo2.iasType == iasValue2.valueType)
    assert(newiasIo2.value.isDefined)
    assert(newiasIo2.value.get == iasValue2.value)
    assert(newiasIo2.mode == iasValue2.mode)
    assert(newiasIo2.fromIasValueValidity.isEmpty)
  }
  
  it must "Update the properties" in {
    val monitoredSysId = new Identifier("MonSysId", IdentifierType.MONITORED_SOFTWARE_SYSTEM, None)
    val pluginId = new Identifier("PluginId", IdentifierType.PLUGIN, Option(monitoredSysId))
    val converterId = new Identifier("ConverterId", IdentifierType.CONVERTER, Some(pluginId))
    val iasioId = new Identifier("IasioId", IdentifierType.IASIO, Option(converterId))
    
    val properties = Map("key1"->"Value1", "key2"->"Value2")
    
    val inOut = InOut.asInput(iasioId,IASTypes.LONG).updateProps(properties)
    val valuePropsOpt = inOut.props
    assert(valuePropsOpt.isDefined)
    assert(valuePropsOpt.get.size==properties.size)
    properties.keys.foreach(key => { 
        assert(valuePropsOpt.get.keys.toList.contains(key))
        val map = inOut.props.get
        val value = map(key)
        assert(properties(key)==value)
    })
    
  }
  
  it must "not update the validity constraint for inputs" in {
    val iasio = InOut.asInput(id, IASTypes.ALARM)
    assertThrows[IllegalArgumentException] {
      iasio.setValidityConstraint(Some(Set("ID")))
    }
  }
  
  it must "update the validity constraint for outputs" in {
    val iasio = InOut.asOutput(id, IASTypes.ALARM)
    val constraints = Some(Set("ID", "ID2"))
    val i2=iasio.setValidityConstraint(constraints)
    assert(i2.validityConstraint.isDefined)
    assert(i2.validityConstraint==constraints)
    
    val i3 = i2.setValidityConstraint(None)
    assert(!i3.validityConstraint.isDefined)
    val i4 = i3.setValidityConstraint(Option(Set.empty))
    assert(!i4.validityConstraint.isDefined)
  }
  
  it must "calculate the validity by time of inputs" in {
    val timeFrame = 3000;
    val inOut = InOut.asInput(id,IASTypes.LONG).updateValueValidity(Some(5L), Some(RELIABLE)).updateProdTStamp(System.currentTimeMillis())
    assert(inOut.getValidityOfInputByTime(timeFrame).iasValidity==RELIABLE,"Shall not be RELIABLE")
    // Give time to invalidate
    Thread.sleep(timeFrame+1000)
    assert(inOut.getValidityOfInputByTime(timeFrame).iasValidity==UNRELIABLE,"Invalid reliablity")
  }
  
  it must "NOT calculate the validity by time of outputs" in {
    val inOut = InOut.asOutput(id,IASTypes.LONG).updateValueValidity(Some(125L), Some(RELIABLE)).updateProdTStamp(System.currentTimeMillis())
    // Calculating the validity by time thoros an exception
    assertThrows[IllegalArgumentException] {
      inOut.getValidityOfInputByTime(1000)
    }
  }

  it must "support arrays of numbers" in {
    val hioArrayLongs: InOut[Long] = InOut.asInput(id, IASTypes.ARRAYOFLONGS)
    val hioArrayDoubles: InOut[Long] = InOut.asInput(id, IASTypes.ARRAYOFDOUBLES)

    // Test the array of Long with java List in the constructor
    val longVals: java.util.List[Long] = CollectionConverters.asJava(List(1L.toLong,2,3,4,5))
    val arrayL = new NumericArray(NumericArray.NumericArrayType.LONG,longVals.asInstanceOf[java.util.List[java.lang.Long]])
    val newArrayL = hioArrayLongs.updateValue(Some(arrayL))
    assert(newArrayL.value.isDefined)
    assert(newArrayL.value.get.isInstanceOf[NumericArray])
    assert(newArrayL.value.get.asInstanceOf[NumericArray].numericArrayType==NumericArray.NumericArrayType.LONG)
    assert(newArrayL.value.get.asInstanceOf[NumericArray].size()==5)
    val elementsL = newArrayL.value.get.asInstanceOf[NumericArray].toArray
    for (i <- 0 until 5) assert(elementsL(i)==longVals.get(i))

    // Test the array of Double with java List in the constructor
    val doubleVals = List(3.1,4.2,5.3)
    val arrayD = new NumericArray(
      NumericArray.NumericArrayType.DOUBLE, CollectionConverters.asJava(doubleVals.toList.map(Double.box)))
    val newArrayD = hioArrayDoubles.updateValue(Some(arrayD))
    assert(newArrayD.value.isDefined)
    assert(newArrayD.value.get.isInstanceOf[NumericArray])
    assert(newArrayD.value.get.asInstanceOf[NumericArray].numericArrayType==NumericArray.NumericArrayType.DOUBLE)
    assert(newArrayD.value.get.asInstanceOf[NumericArray].size()==3)
    val elementsD = newArrayD.value.get.asInstanceOf[NumericArray].toArray
    for (i <- 0 until 3) assert(elementsD(i)==doubleVals(i).toDouble)

    // Test the array of Long with scala List in the constructor
    val scalaLongVals: List[Long] = List(1,2,3,4,5)
    val arrayLS = new NumericArray(scalaLongVals,NumericArray.NumericArrayType.LONG)
    val newArrayLS = hioArrayLongs.updateValue(Some(arrayLS))
    assert(newArrayLS.value.isDefined)
    assert(newArrayLS.value.get.isInstanceOf[NumericArray])
    assert(newArrayLS.value.get.asInstanceOf[NumericArray].numericArrayType==NumericArray.NumericArrayType.LONG)
    assert(newArrayLS.value.get.asInstanceOf[NumericArray].size()==5)
    val elementsLS = newArrayLS.value.get.asInstanceOf[NumericArray].toArray
    for (i <- 0 until 5) assert(elementsLS(i)==scalaLongVals(i))

    // Test the array of Double with java List in the constructor
    val scalaDoubleVals: List[Double] = List(3.1,4.2,5.3)
    val arrayDS = new NumericArray(scalaDoubleVals,NumericArray.NumericArrayType.DOUBLE)
    val newArrayDS = hioArrayDoubles.updateValue(Some(arrayDS))
    assert(newArrayDS.value.isDefined)
    assert(newArrayDS.value.get.isInstanceOf[NumericArray])
    assert(newArrayDS.value.get.asInstanceOf[NumericArray].numericArrayType==NumericArray.NumericArrayType.DOUBLE)
    assert(newArrayDS.value.get.asInstanceOf[NumericArray].size()==3)
    val elementsDS = newArrayDS.value.get.asInstanceOf[NumericArray].toArray
    for (i <- 0 until 3) assert(elementsDS(i)==scalaDoubleVals(i))
  }

}