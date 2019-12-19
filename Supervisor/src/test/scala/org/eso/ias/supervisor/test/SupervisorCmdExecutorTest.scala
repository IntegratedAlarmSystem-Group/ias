package org.eso.ias.supervisor.test

import java.util.Optional

import org.eso.ias.cdb.json.{CdbJsonFiles, JsonReader}
import org.eso.ias.cdb.pojos.{DasuDao, SupervisorDao}
import org.eso.ias.command.{CommandExitStatus, CommandMessage, CommandType}
import org.eso.ias.logging.IASLogger
import org.eso.ias.supervisor.SupervisorCmdExecutor
import org.scalatest.FlatSpec

import scala.collection.JavaConverters

/**
 * Tests the SupervisorCmdExecutor using the SupervisorID configuration in the CDB
 */
class SupervisorCmdExecutorTest extends FlatSpec {

  /** The logger */
  private val logger = IASLogger.getLogger(this.getClass);

  val supervisorId = "SupervisorID"

  /** CDB reader */
  val cdbReader = new JsonReader(new CdbJsonFiles("."))

  val supervisorDao: Optional[SupervisorDao] = cdbReader.getSupervisor(supervisorId)
  assert(supervisorDao.isPresent, "Cannot read CDB definition of supervisor "+supervisorId)

  /** The DASUs deployed in the Supervisor */
  val dasuDaos: Set[DasuDao] = {
    val dtds = JavaConverters.asScalaSet(supervisorDao.get().getDasusToDeploy).toSet
    dtds.map(dtd => dtd.getDasu)
  }
  assert(dasuDaos.size==3,"Wrong num. of DASUs from cdb reader")

  /** The executor to test */
  val cmdExecutor = new SupervisorCmdExecutor(dasuDaos)

  behavior of "The SupervisorCmdExecutor"

  it must "correctly record the names of the outputs of the DASUs" in {
    val outputs = cmdExecutor.outputOfDasus

    assert(outputs.size==3,"Wrong size of map of outputs "+outputs.size)

    val dasuOfOutput1 = outputs("Dasu1-OutID")
    assert(dasuOfOutput1=="Dasu1")

    val dasuOfOutput2 = outputs("Dasu2-OutID")
    assert(dasuOfOutput2=="Dasu2")

    val dasuOfOutput3 = outputs("Dasu3-OutID")
    assert(dasuOfOutput3=="Dasu3")
  }

  it must "correctly record the IDs of all the TFs" in {
    assert(cmdExecutor.usedTfs.size==2)
    assert(cmdExecutor.usedTfs.contains("org.eso.ias.asce.transfer.impls.MultiplicityTF"))
    assert(cmdExecutor.usedTfs.contains("org.eso.ias.asce.transfer.impls.MinMaxThresholdTF"))
  }

  behavior of "The execution of TF_CHANGED command"

  it must "return OK and ask for a RESTART when the TF runs in one of its ASCE" in {
    val params = {
      val temp = JavaConverters.asJavaCollection(List("org.eso.ias.asce.transfer.impls.MinMaxThresholdTF"))
      val ret = new java.util.Vector[String]()
      ret.addAll(temp)
      ret
    }

    val cmd = new CommandMessage(
      "senderId",
      "destId",
      CommandType.TF_CHANGED,
      1L,
      params,
      System.currentTimeMillis(),
      null
    )
    val ret = cmdExecutor.tfChanged(cmd)
    assert(ret.mustRestart)
    assert(!ret.mustShutdown)
    assert(ret.status==CommandExitStatus.OK)
  }

  it must "return REJECTED and not ask for a RESTART when the TF in one of its ASCE" in {
    val params = {
      val temp = JavaConverters.asJavaCollection(List("org.eso.ias.asce.transfer.impls.MinMaxThresholdTF"))
      val ret = new java.util.Vector[String]()
      ret.addAll(temp)
      ret
    }

    val cmd = new CommandMessage(
      "senderId",
      "destId",
      CommandType.TF_CHANGED,
      1L,
      params,
      System.currentTimeMillis(),
      null
    )
    val ret = cmdExecutor.tfChanged(cmd)
    assert(ret.mustRestart)
    assert(!ret.mustShutdown)
    assert(ret.status==CommandExitStatus.OK)
  }

}
