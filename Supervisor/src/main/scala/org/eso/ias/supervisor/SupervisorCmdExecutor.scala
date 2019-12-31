package org.eso.ias.supervisor

import org.eso.ias.command.CommandListener.CmdExecutionResult
import org.eso.ias.command.{CommandExitStatus, CommandListener, CommandMessage, DefaultCommandExecutor}

import scala.collection.JavaConverters

/**
 * The executor of commands for the Supervisor.
 *
 * The SupervisorCmdExecutor extends the DefaultCommandExecutor to customize the
 * TF_CHANGED command.
 *
 * @param tfIDs The list of the TFs running in the ASCEs of the Supervisor
 */
class SupervisorCmdExecutor(
                             tfIDs: List[String]
                           ) extends DefaultCommandExecutor {
  /**
   * A TF has been changed.
   *
   * The Supervisor must restart if the changed TF is used by at least one of its ASCEs so that the new
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

    if (tfIDs.contains(idOfTF)) {
      // One ASCE is using the TF => request a restart
      new CmdExecutionResult(CommandExitStatus.OK,null,false,true)
    } else {
      // No ASCE is using the TF => reject the command
      val props = JavaConverters.mapAsJavaMap(Map("Reason" -> ("Unused TF " + idOfTF)))
      new CmdExecutionResult(CommandExitStatus.REJECTED, null, false, false)
    }
  }
}
