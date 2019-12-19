package org.eso.ias.supervisor

import org.eso.ias.cdb.pojos.DasuDao
import org.eso.ias.command.CommandListener.CmdExecutionResult
import org.eso.ias.command.{CommandExitStatus, CommandListener, CommandMessage, DefaultCommandExecutor}

import scala.collection.JavaConverters

/**
 * The executor of commands for the Supervisor.
 *
 * The SupervisorCmdExecutor extends the DefaultCommandExecutor to customize the
 * TF_CHANGED command.
 *
 * @param dasus The DASUs deployed in the Supervisor
 */
class SupervisorCmdExecutor(
                           dasus: Set[DasuDao]
                           ) extends DefaultCommandExecutor {


  /** The ids of the TFs used by the ASCEs deployed in the Supervisor */
  lazy val usedTfs: Set[String] = {
    dasus.foldLeft(Set[String]())( (s, d) => {
      val ascesOfDasu = JavaConverters.asScalaSet(d.getAsces).toSet;
      val tfsOfAsces: Set[String] = ascesOfDasu.map( _.getTransferFunction.getClassName)
      s++tfsOfAsces
    })
  }

  /**
   * The outputs of the DASUs deployed in the Supervisor
   *
   * The key is the ID of the output and the value is the DASU that produces it
   */
  lazy val outputOfDasus = {
    dasus.foldLeft(Map[String, String]())( (m,d) => {
      m.+(d.getOutput.getId -> d.getId)
    })
  }

  /**
   * A TF has been changed.
   *
   * The Supervisor must restart if the changed TF is used by one of its ASCEs so that the new
   * TF is reloaded.
   *
   * @param cmd The TF_CHANGED command received from the command topic
   * @return The result of the execution of the command
   * @throws Exception
   */
  override def tfChanged(cmd: CommandMessage): CommandListener.CmdExecutionResult = {
    require(Option(cmd).nonEmpty,"No command to execute")
    require(Option(cmd.getParams).nonEmpty && cmd.getParams.size()==1,
      "Invalid parameters of RESTART")
    val idOfTF = cmd.getParams.get(0)

    if (usedTfs.contains(idOfTF)) {
      // One ASCE is using the TF => request a restart
      new CmdExecutionResult(CommandExitStatus.OK,null,false,true)
    } else {
      // No ASCE is using the TF => reject the command
      val props = JavaConverters.mapAsJavaMap(Map("Reason" -> ("Unused TF " + idOfTF)))
      new CmdExecutionResult(CommandExitStatus.REJECTED, null, false, false)
    }
  }
}
