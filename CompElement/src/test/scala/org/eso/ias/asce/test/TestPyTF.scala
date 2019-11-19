package org.eso.ias.asce.test

import java.util.Properties

import org.eso.ias.asce.transfer.{JavaTransfer, TransferFunctionLanguage, TransferFunctionSetting}
import org.eso.ias.asce.{AsceStates, CompEleThreadFactory, ComputingElement}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types._
import org.scalatest.FlatSpec

import scala.collection.mutable.{Map => MutableMap}
import scala.util.Try

/**
 * Test the python tranfer function
 *
 * The test is done with the TF in TestTransferFunction.py that
 * - sets the output to the same value of the input
 * - sets the mode to the same mode of the input
 * - sets the properties of the output to the properties passed by the ASCE
 *
 * The test is repeated for each possible IASIO type
 */
class TestPyTF extends FlatSpec {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass)

  /** The ID of the DASU where the components runs */
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)
  val dasId = new Identifier("DAS-ID",IdentifierType.DASU,supervId)

  /** The ID of the component running into the DASU */
  val compID = new Identifier("COMP-ID",IdentifierType.ASCE,Option(dasId))

  // The ID of the output generated by the component
  val outId = new Identifier("OutputId",IdentifierType.IASIO,Option(compID))

  val threadFactory: CompEleThreadFactory = new CompEleThreadFactory("Test-runningId")

  val numOfInputs = 1

  val inputIDLong = "MPointID-LONG"
  val mpIdLong = new Identifier(inputIDLong,IdentifierType.IASIO,Option(compID))
  val mpLong = InOut.asInput(mpIdLong,IASTypes.LONG).updateValue(Some(1L)).updateProdTStamp(System.currentTimeMillis())
  val outputLong: InOut[java.lang.Long] = InOut.asOutput(outId, IASTypes.LONG)

  val inputIDInt = "MPointID-INT"
  val mpIdInt = new Identifier(inputIDInt,IdentifierType.IASIO,Option(compID))
  val mpInt = InOut.asInput(mpIdInt,IASTypes.INT).updateValue(Some(1)).updateProdTStamp(System.currentTimeMillis())
  val outputInt: InOut[java.lang.Integer] = InOut.asOutput(outId, IASTypes.INT)

  val inputIDShort = "MPointID-SHORT"
  val mpIdShort = new Identifier(inputIDShort,IdentifierType.IASIO,Option(compID))
  val mpShort = InOut.asInput(mpIdShort,IASTypes.SHORT).updateValue(Some(1: Short)).updateProdTStamp(System.currentTimeMillis())
  val outputShort: InOut[java.lang.Short] = InOut.asOutput(outId, IASTypes.SHORT)

  val inputIDByte = "MPointID-BYTE"
  val mpIdByte = new Identifier(inputIDByte,IdentifierType.IASIO,Option(compID))
  val mpByte = InOut.asInput(mpIdByte,IASTypes.BYTE).updateValue(Some(1: Byte)).updateProdTStamp(System.currentTimeMillis())
  val outputByte: InOut[java.lang.Byte] = InOut.asOutput(outId, IASTypes.BYTE)

  val inputIDDouble = "MPointID-DOUBLE"
  val mpIdDouble = new Identifier(inputIDDouble,IdentifierType.IASIO,Option(compID))
  val mpDouble = InOut.asInput(mpIdDouble,IASTypes.DOUBLE).updateValue(Some(1.5D)).updateProdTStamp(System.currentTimeMillis())
  val outputDouble: InOut[java.lang.Double] = InOut.asOutput(outId, IASTypes.DOUBLE)

  val inputIDFloat = "MPointID-FLOAT"
  val mpIdFloat = new Identifier(inputIDFloat,IdentifierType.IASIO,Option(compID))
  val mpFloat = InOut.asInput(mpIdFloat,IASTypes.FLOAT).updateValue(Some(1.5f)).updateProdTStamp(System.currentTimeMillis())
  val outputFloat: InOut[java.lang.Float] = InOut.asOutput(outId, IASTypes.FLOAT)

  val inputIDBool = "MPointID-BOOLEAN"
  val mpIdBool = new Identifier(inputIDBool,IdentifierType.IASIO,Option(compID))
  val mpBool = InOut.asInput(mpIdBool,IASTypes.BOOLEAN).updateValue(Some(java.lang.Boolean.TRUE)).updateProdTStamp(System.currentTimeMillis())
  val outputBool: InOut[java.lang.Boolean] = InOut.asOutput(outId, IASTypes.BOOLEAN)

  val inputIDChar = "MPointID-CHAR"
  val mpIdChar = new Identifier(inputIDChar,IdentifierType.IASIO,Option(compID))
  val mpChar = InOut.asInput(mpIdChar,IASTypes.CHAR).updateValue(Some('C')).updateProdTStamp(System.currentTimeMillis())
  val outputChar: InOut[java.lang.Character] = InOut.asOutput(outId, IASTypes.CHAR)

  val inputIDString = "MPointID-STRING"
  val mpIdString = new Identifier(inputIDString,IdentifierType.IASIO,Option(compID))
  val mpString = InOut.asInput(mpIdString,IASTypes.STRING).updateValue(Some("String")).updateProdTStamp(System.currentTimeMillis())
  val outputString: InOut[java.lang.String] = InOut.asOutput(outId, IASTypes.STRING)

  val inputIDTStamp = "MPointID-TIMESTAMP"
  val mpIdTStamp = new Identifier(inputIDTStamp,IdentifierType.IASIO,Option(compID))
  val mpTStamp = InOut.asInput(mpIdTStamp,IASTypes.TIMESTAMP).updateValue(Some(System.currentTimeMillis())).updateProdTStamp(System.currentTimeMillis())
  val outputTStamp: InOut[java.lang.Long] = InOut.asOutput(outId, IASTypes.TIMESTAMP)

  val inputIDArrayDoubles = "MPointID-ARRAYOFDOUBLES"
  val mpIdArrayDoubles = new Identifier(inputIDArrayDoubles,IdentifierType.IASIO,Option(compID))
  val arrayDoubles = new NumericArray(List(1.1D,2.2D,3.3D,4.4D,5.5D),NumericArray.NumericArrayType.DOUBLE)
  val mpArrayDoubles = InOut.asInput(mpIdArrayDoubles,IASTypes.ARRAYOFDOUBLES).updateValue(Some(arrayDoubles)).updateProdTStamp(System.currentTimeMillis())
  val outputArrayDoubles: InOut[NumericArray] = InOut.asOutput(outId, IASTypes.ARRAYOFDOUBLES)

  val inputIDArrayLongs = "MPointID-ARRAYOFLONGS"
  val mpIdArrayLongs = new Identifier(inputIDArrayLongs,IdentifierType.IASIO,Option(compID))
  val arrayLongs = new NumericArray(List(1L,2L,3L,4L,5L),NumericArray.NumericArrayType.LONG)
  val mpArrayLongs = InOut.asInput(mpIdArrayLongs,IASTypes.ARRAYOFLONGS).updateValue(Some(arrayLongs)).updateProdTStamp(System.currentTimeMillis())
  val outputArrayLongs: InOut[NumericArray] = InOut.asOutput(outId, IASTypes.ARRAYOFLONGS)

  val inputIDAlarm = "MPointID-ALARM"
  val mpIdAlarm = new Identifier(inputIDAlarm,IdentifierType.IASIO,Option(compID))
  val mpAlarm = InOut.asInput(mpIdAlarm,IASTypes.ALARM).updateValue(Some(Alarm.SET_LOW)).updateProdTStamp(System.currentTimeMillis())
  val outputAlarm: InOut[Alarm] = InOut.asOutput(outId, IASTypes.ALARM)

  // The threshold to assess the validity from the arrival time of the input
  val validityThresholdInSecs = 2

  // Instantiate on ASCE with a java TF implementation
  val pythonTFSetting =new TransferFunctionSetting(
    "TestTF.TestTransferFunction",
    TransferFunctionLanguage.python,
    None,
    threadFactory)

  behavior of "The python Transfer Function"

  it must "load and initialize the python object" in {
    logger.info("Testing initialize")
    val inputsMPs: MutableMap[String, InOut[_]] = MutableMap[String, InOut[_]]()
    inputsMPs+=(mpIdAlarm.id -> mpAlarm)

    val javaComp: ComputingElement[Alarm] = new ComputingElement[Alarm](
      compID,
      outputAlarm,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[Alarm]



    val ret = javaComp.initialize()
    logger.info("Initialize tested")
  }

  it must "run the TF" in {
    logger.info("Testing python eval(...)")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdAlarm.id -> mpAlarm)

    val javaComp: ComputingElement[Alarm] = new ComputingElement[Alarm](
      compID,
      outputAlarm,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[Alarm]

    val ret = javaComp.initialize()
    logger.info("Python eval(...) tested")
    assert(ret==AsceStates.InputsUndefined)

    val newOut: Try[InOut[Alarm]] = javaComp.transfer(inputsMPs,compID,outputAlarm)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[Alarm] = newOut.get
    assert (out.iasType==IASTypes.ALARM,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==Alarm.SET_LOW)

    assert (out.id.id==outId.id,"Unexpected output ID")
    assert(out.id.fullRunningID==outId.fullRunningID,"Unexpected output full running ID")
    logger.info("TF execution tested")
  }

  /**
   * Test the setting of additional properties in the output
   * The python TF sets the properties of the input in the output
   */
  it must "Set the properties in the output" in {
    logger.info("Test setting of additional properties")

    val additionalPros = Map("key1" -> "Value1","key2" -> "Value2","key3" -> "Value3")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdAlarm.id -> mpAlarm.updateProps(additionalPros))

    val javaComp: ComputingElement[Alarm] = new ComputingElement[Alarm](
      compID,
      outputAlarm,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[Alarm]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[Alarm]] = javaComp.transfer(inputsMPs,compID,outputAlarm)
    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[Alarm] = newOut.get
    val recvPropsOpt = out.props
    assert (recvPropsOpt.isDefined,"No additional properties recived")
    val recvProps = recvPropsOpt.get
    assert(recvProps.size==3,"Wrong number of props received")
    for (i <- 1 to 3) {
      val key = "key"+i
      val valueOpt = recvProps.get(key)
      assert(valueOpt.isDefined,"Value of "+key+" not found")
      assert(valueOpt.get=="Value"+i)
    }
  }

  it must "support input of type LONG" in {
    logger.info("Testing support of input type LONG")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdLong.id -> mpLong.updateValue(Some(25L)))

    val javaComp: ComputingElement[java.lang.Long] = new ComputingElement[java.lang.Long](
      compID,
      outputLong,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Long]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Long]] = javaComp.transfer(inputsMPs,compID,outputLong)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Long] = newOut.get
    assert (out.iasType==IASTypes.LONG,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==25L)

    logger.info("Input type LONG tested")
  }

  it must "support input of type INT" in {
    logger.info("Testing support of input type INT")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdInt.id -> mpInt.updateValue(Some(25)))

    val javaComp: ComputingElement[java.lang.Integer] = new ComputingElement[java.lang.Integer](
      compID,
      outputInt,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Integer]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Integer]] = javaComp.transfer(inputsMPs,compID,outputInt)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Integer] = newOut.get
    assert (out.iasType==IASTypes.INT,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==25)

    logger.info("Input type INT tested")
  }
  it must "support input of type SHORT" in {
    logger.info("Testing support of input type SHORT")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdShort.id -> mpShort.updateValue(Some(5: Short)))

    val javaComp: ComputingElement[java.lang.Short] = new ComputingElement[java.lang.Short](
      compID,
      outputShort,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Short]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Short]] = javaComp.transfer(inputsMPs,compID,outputShort)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Short] = newOut.get
    assert (out.iasType==IASTypes.SHORT,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==5)

    logger.info("Input type SHORT tested")
  }

  it must "support input of type BYTE" in {
    logger.info("Testing support of input type BYTE")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdByte.id -> mpByte.updateValue(Some(8: Byte)))

    val javaComp: ComputingElement[java.lang.Byte] = new ComputingElement[java.lang.Byte](
      compID,
      outputByte,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Byte]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Byte]] = javaComp.transfer(inputsMPs,compID,outputByte)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Byte] = newOut.get
    assert (out.iasType==IASTypes.BYTE,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==8)

    logger.info("Input type BYTE tested")
  }

  it must "support input of type DOUBLE" in {
    logger.info("Testing support of input type DOUBLE")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdDouble.id -> mpDouble.updateValue(Some(5.5D)))

    val javaComp: ComputingElement[java.lang.Double] = new ComputingElement[java.lang.Double](
      compID,
      outputDouble,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Double]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Double]] = javaComp.transfer(inputsMPs,compID,outputDouble)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Double] = newOut.get
    assert (out.iasType==IASTypes.DOUBLE,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==5.5D)

    logger.info("Input type DOUBLE tested")
  }

  it must "support input of type FLOAT" in {
    logger.info("Testing support of input type FLOAT")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdFloat.id -> mpFloat.updateValue(Some(3.3f)))

    val javaComp: ComputingElement[java.lang.Float] = new ComputingElement[java.lang.Float](
      compID,
      outputFloat,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Float]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Float]] = javaComp.transfer(inputsMPs,compID,outputFloat)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Float] = newOut.get
    assert (out.iasType==IASTypes.FLOAT,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==3.3f)

    logger.info("Input type FLOAT tested")
  }

  it must "support input of type BOOLEAN" in {
    logger.info("Testing support of input type BOOLEAN")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdBool.id -> mpBool.updateValue(Some(true)))

    val javaComp: ComputingElement[java.lang.Boolean] = new ComputingElement[java.lang.Boolean](
      compID,
      outputBool,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Boolean]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Boolean]] = javaComp.transfer(inputsMPs,compID,outputBool)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Boolean] = newOut.get
    assert (out.iasType==IASTypes.BOOLEAN,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==true)

    logger.info("Input type BOOLEAN tested")
  }

  it must "support input of type CHAR" in {
    logger.info("Testing support of input type CHAR")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdChar.id -> mpChar.updateValue(Some('T')))

    val javaComp: ComputingElement[java.lang.Character] = new ComputingElement[java.lang.Character](
      compID,
      outputChar,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Character]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Character]] = javaComp.transfer(inputsMPs,compID,outputChar)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Character] = newOut.get
    assert (out.iasType==IASTypes.CHAR,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get=='T')

    logger.info("Input type CHAR tested")
  }

  it must "support input of type STRING" in {
   logger.info("Testing support of input type STRING")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdString.id -> mpString.updateValue(Some("Test")))

    val javaComp: ComputingElement[java.lang.String] = new ComputingElement[java.lang.String](
      compID,
      outputString,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.String]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.String]] = javaComp.transfer(inputsMPs,compID,outputString)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.String] = newOut.get
    assert (out.iasType==IASTypes.STRING,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get=="Test")

    logger.info("Input type STRING tested")
  }

  it must "support input of type TIMESTAMP" in {
    logger.info("Testing support of input type TIMESTAMP")
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdTStamp.id -> mpTStamp.updateValue(Some(1234567890L)))

    val javaComp: ComputingElement[java.lang.Long] = new ComputingElement[java.lang.Long](
      compID,
      outputTStamp,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[java.lang.Long]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[java.lang.Long]] = javaComp.transfer(inputsMPs,compID,outputTStamp)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[java.lang.Long] = newOut.get
    assert (out.iasType==IASTypes.TIMESTAMP,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==1234567890L)

    logger.info("Input type TIMESTAMP tested")
  }

  it must "support input of type ARRAYOFDOUBLES" in {
    logger.info("Testing support of input type ARRAYOFDOUBLES")
    val inputArray: NumericArray = new NumericArray(List(1.2D,2.3D,3.4D),NumericArray.NumericArrayType.DOUBLE)
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdArrayDoubles.id -> mpArrayDoubles.updateValue(Some(inputArray)))

    val javaComp: ComputingElement[NumericArray] = new ComputingElement[NumericArray](
      compID,
      outputArrayDoubles,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[NumericArray]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[NumericArray]] = javaComp.transfer(inputsMPs,compID,outputArrayDoubles)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[NumericArray] = newOut.get
    assert (out.iasType==IASTypes.ARRAYOFDOUBLES,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==inputArray)

    logger.info("Input type ARRAYOFDOUBLES tested")
  }

  it must "support input of type ARRAYOFLONGS" in {
    logger.info("Testing support of input type ARRAYOFLONGS")
    val inputArray: NumericArray = new NumericArray(List(1L,2L,3L),NumericArray.NumericArrayType.LONG)
    val inputsMPs: Map[String, InOut[_]] = Map(mpIdArrayLongs.id -> mpArrayLongs.updateValue(Some(inputArray)))

    val javaComp: ComputingElement[NumericArray] = new ComputingElement[NumericArray](
      compID,
      outputArrayLongs,
      inputsMPs.values.toSet,
      pythonTFSetting,
      validityThresholdInSecs,
      new Properties()) with JavaTransfer[NumericArray]

    val ret = javaComp.initialize()

    val newOut: Try[InOut[NumericArray]] = javaComp.transfer(inputsMPs,compID,outputArrayLongs)

    assert(newOut.isSuccess,"Exception got from the TF")

    val out: InOut[NumericArray] = newOut.get
    assert (out.iasType==IASTypes.ARRAYOFLONGS,"The TF produced a value of the worng type "+out.iasType )
    assert(out.value.isDefined)
    assert(out.value.get==inputArray)

    logger.info("Input type ARRAYOFLONGS tested")
  }

  it must "support input of type ALARM" in {}
  logger.info("Testing support of input type ALARM")
  val inputsMPs: Map[String, InOut[_]] = Map(mpIdAlarm.id -> mpAlarm.updateValue(Some(Alarm.SET_HIGH)))

  val javaComp: ComputingElement[Alarm] = new ComputingElement[Alarm](
    compID,
    outputAlarm,
    inputsMPs.values.toSet,
    pythonTFSetting,
    validityThresholdInSecs,
    new Properties()) with JavaTransfer[Alarm]

  val ret = javaComp.initialize()

  val newOut: Try[InOut[Alarm]] = javaComp.transfer(inputsMPs,compID,outputAlarm)

  assert(newOut.isSuccess,"Exception got from the TF")

  val out: InOut[Alarm] = newOut.get
  assert (out.iasType==IASTypes.ALARM,"The TF produced a value of the worng type "+out.iasType )
  assert(out.value.isDefined)
  assert(out.value.get==Alarm.SET_HIGH)

  logger.info("Input type ALARM tested")
}
