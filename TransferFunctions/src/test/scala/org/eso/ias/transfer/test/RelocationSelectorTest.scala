package org.eso.ias.transfer.test

import java.util.Properties
import scala.compiletime.uninitialized

import com.typesafe.scalalogging.Logger
import org.eso.ias.asce.exceptions.UnexpectedNumberOfInputsException
import org.eso.ias.asce.transfer.{IasIO, IasioInfo}
import org.eso.ias.logging.IASLogger
import org.eso.ias.tranfer.RelocationSelector
import org.eso.ias.types._
import org.scalatest.{BeforeAndAfterEach}
import org.scalatest.flatspec.AnyFlatSpec

class RelocationSelectorTest extends AnyFlatSpec with BeforeAndAfterEach {

  /** The logger */
  val logger: Logger = IASLogger.getLogger(this.getClass)

  /**  The ID of the SUPERVISOR where the components runs */
  val supervId = new Identifier("SupervId",IdentifierType.SUPERVISOR,None)

  /**  The ID of the DASU where the components runs */
  val dasuId = new Identifier("DasuId",IdentifierType.DASU,supervId)

  /** The ID of the component running into the DASU */
  val compID = new Identifier("ASCE-ID-ForTest",IdentifierType.ASCE,Option(dasuId))

  /** The ID of the output generated by the component */
  val outId = new Identifier("OutputId",IdentifierType.IASIO, Some(compID))

  /** the IASIO in output */
  val out: IasIO[Double] = new IasIO(InOut.asOutput(outId,IASTypes.DOUBLE))

  /** The ID of one IASIO */
  val aId = new Identifier("A",IdentifierType.IASIO, Some(compID))

  /** The ID of the A input */
  val a: IasIO[Double] = buildInput(aId,2.2d)

  /** The ID of one IASIO */
  val bId = new Identifier("B",IdentifierType.IASIO, Some(compID))

  /** The ID of the B input */
  val b: IasIO[Double] = buildInput(bId,3.3d)

  /** The ID of one IASIO */
  val cId = new Identifier("C",IdentifierType.IASIO, Some(compID))

  /** The ID of the C input */
  val c: IasIO[Double] = buildInput(cId,4.4d)

  /** The time frame for the validity */
  val validityTimeFrame = 2000

  /** The TF to test */
  var tf: RelocationSelector[Double] = uninitialized

  /**
    *  Build the IASIO in input with the passed id, type and timestamp
    *
    *  @param id The identifier of the input
    *  @param value initial value
    *  @param tStamp the daus production timestamp
    *  @param mode the operational mode
    */
  def buildInput(
                  id: Identifier,
                  value: Double,
                  tStamp: Long = System.currentTimeMillis(),
                  mode: OperationalMode = OperationalMode.OPERATIONAL,
                  validity: IasValidity = IasValidity.RELIABLE): IasIO[Double] = {

    val inout: InOut[Double] =
      InOut.asInput(id,IASTypes.DOUBLE)
        .updateProdTStamp(tStamp)
        .updateValueValidity(Some(value),Some(Validity(validity)))
    new IasIO(inout).updateMode(mode)
  }

  override def beforeEach() : scala.Unit = {
    tf = new RelocationSelector[Double](compID.id,compID.fullRunningID,validityTimeFrame,new Properties())
  }

  behavior of "The RelocationSelector transfer function"

  it must "throws exception in intialize if gets only 1 input" in {
    val inputs: Set[IasIO[?]] = Set (a)
    val inputInfos = inputs.map (iasio => new IasioInfo(iasio.id,iasio.iasType))
    assertThrows[UnexpectedNumberOfInputsException] {
      tf.initialize(inputInfos,new IasioInfo(out.id,out.iasType))
    }
  }

  it must "successfully initialize with correct IDs" in {
    val inputs: Set[IasIO[?]] = Set (a,b,c)
    val inputInfos = inputs.map (iasio => new IasioInfo(iasio.id,iasio.iasType))
    tf.initialize(inputInfos,new IasioInfo(out.id,out.iasType))
  }

  it must "produce a invalid output when no input is valid" in {
    val inputs = Map(a.id -> a, b.id -> b, c.id -> c)
    val res = tf.eval(inputs, out)

    logger.info("Output {} {}",res.mode, res.validity, res.value.getOrElse("N/A"))

    assert(res.validity==IasValidity.UNRELIABLE)
    assert(res.value.isDefined)
    assert(res.validityConstraints.nonEmpty)

    // Give time to invalidate
    Thread.sleep(validityTimeFrame+100)
    val res2 = tf.eval(inputs, out)
    logger.info("Output {} {}",res2.mode, res2.validity, res2.value.getOrElse("N/A"))

    assert(res2.validity==IasValidity.UNRELIABLE)
    assert(res2.value.isDefined)
    assert(res2.validityConstraints.isEmpty)
  }

  it must "produce the second output when the others are not valid" in {

    val inputs = Map(a.id -> a, b.id -> b, c.id -> c)
    val resNotUseful = tf.eval(inputs, out)

    // Give time to invalidate
    Thread.sleep(validityTimeFrame+100)

    val inputsInvalidByTime = Map(
      a.id -> a,
      b.id -> buildInput(bId,3.3d),
      c.id -> c)
    val res = tf.eval(inputsInvalidByTime, out)

    assert(res.value.isDefined)
    assert(res.value.get==3.3d)
    assert(res.id==outId.id)
    assert(res.validityConstraints.isDefined)
    assert(res.validityConstraints.get.size==1)
    assert(res.validityConstraints.get.contains(b.id))

    // Give time to invalidate
    Thread.sleep(validityTimeFrame+100)

    val inputs2 = Map(
      a.id -> a,
      b.id -> b,
      c.id -> buildInput(cId,4.4d, mode=OperationalMode.MALFUNCTIONING))
    val res2 = tf.eval(inputs2, out)
    assert(res2.value.isDefined)
    assert(res2.value.get==4.4d)
    assert(res2.id==outId.id)
    assert(res2.mode==OperationalMode.MALFUNCTIONING)
    assert(res2.validityConstraints.isDefined)
    assert(res2.validityConstraints.get.size==1)
    assert(res2.validityConstraints.get.contains(c.id))
  }
}

