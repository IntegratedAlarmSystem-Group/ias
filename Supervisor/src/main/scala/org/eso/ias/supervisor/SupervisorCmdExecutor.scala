package org.eso.ias.supervisor

import com.typesafe.scalalogging.Logger
import org.eso.ias.command.CommandListener.CmdExecutionResult
import org.eso.ias.command.{CommandExitStatus, CommandListener, CommandMessage, DefaultCommandExecutor}
import org.eso.ias.dasu.Dasu
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.{Identifier, IdentifierType}

import java.util.Objects
import scala.jdk.javaapi.CollectionConverters
import scala.util.Try

/**
 * The executor of commands for the Supervisor.
 *
 * The SupervisorCmdExecutor extends the DefaultCommandExecutor to customize the
 * TF_CHANGED command.
 *
 * @param tfIDs The list of the TFs running in the ASCEs of the Supervisor
 */
class SupervisorCmdExecutor(tfIDs: List[String], dasus: Map[String, Dasu])
  extends DefaultCommandExecutor {
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
  @throws(classOf[Exception])
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
      val props = CollectionConverters.asJava(Map("Reason" -> ("Unused TF " + idOfTF)))
      new CmdExecutionResult(CommandExitStatus.REJECTED, null, false, false)
    }
  }

  /**
     * A an Alarm has been acknowledged.
     *
     * This message is aimed to the DASU that produces such alarm (i.e. received by the Supervisor
     * where the DASU is deployed) all the other tools can safely ignore the command.
     *
     * This implementation does nothing and return OK.
     *
     * @param cmd The ACK command received from the command topic
     * @return The result of the execution of the command
     * @throws Exception
     */
     @throws(classOf[Exception])
     override def alarmAcknowledged(cmd: CommandMessage): CmdExecutionResult = {
       require(Objects.nonNull(cmd), "Null command to execute")
       SupervisorCmdExecutor.logger.info("ACK command received from {}", cmd.getSenderFullRunningId)

       // Did we get the expected number of params?
       if (Objects.isNull(cmd.getParams)) {
         SupervisorCmdExecutor.logger.error("Got a null list of params for the ACK from {}",cmd.getSenderFullRunningId)
         new CmdExecutionResult(CommandExitStatus.ERROR, null, false, false)
       } else if (cmd.getParams.size()!=cmd.getCommand.expectedNumOfParameters) {
         SupervisorCmdExecutor.logger.error("Wrong number of params for the ACK from {} (exp. {} but was {})",
           cmd.getSenderFullRunningId,
           cmd.getCommand.expectedNumOfParameters,
           cmd.getParams.size())
           new CmdExecutionResult(CommandExitStatus.ERROR, null, false, false)
       } else {
         val tryToConvertAlarmId = Try[Identifier] {Identifier.apply(cmd.getParams.get(0))}
         if (tryToConvertAlarmId.isFailure) {
           SupervisorCmdExecutor.logger.error("Malformed full running ID of the alarm to ACK: [{}",cmd.getParams.get(0))
           return new CmdExecutionResult(CommandExitStatus.ERROR, null, false, false)
         }
         val alarmToAck = tryToConvertAlarmId.get
         val userComment = cmd.getParams.get(1) // The Supervisor does nothing with the user comment
         SupervisorCmdExecutor.logger.debug("{} alarm to ACK with user comment {}", alarmToAck, userComment)

         val dasuIdOpt = alarmToAck.getIdOfType(IdentifierType.DASU)
         if (dasuIdOpt.isEmpty) {
           SupervisorCmdExecutor.logger.error("Missing DASU in the full running ID of the alarm to ACK: [{}",cmd.getParams.get(0))
           return new CmdExecutionResult(CommandExitStatus.ERROR, null, false, false)
         }
         val dasuId = dasuIdOpt.get
         val dasuOpt = dasus.get(dasuId)
         if (dasuOpt.isEmpty) {
           SupervisorCmdExecutor.logger.error("DASU ID {} of the alarm to ACK not found in this supervisor",dasuId)
           return new CmdExecutionResult(CommandExitStatus.ERROR, null, false, false)
         }
         val dasu = dasuOpt.get
         val acked = dasu.ack(alarmToAck)
         if (acked) {
           new CmdExecutionResult(CommandExitStatus.OK, null, false, false);
         } else {
           new CmdExecutionResult(CommandExitStatus.ERROR, null, false, false)
         }
       }
     }
}

object SupervisorCmdExecutor {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(SupervisorCmdExecutor.getClass)
}