package org.eso.ias.supervisor.test

import com.typesafe.scalalogging.Logger
import org.eso.ias.command.{CommandListener, CommandManager}
import org.eso.ias.logging.IASLogger
import org.eso.ias.types.Identifier

class CommandManagerMock(fullRunningId: String, id: String) extends CommandManager(fullRunningId,id) {

  /**
   * Secondary constructor
   *
   * @param identifier The identifier
   */
  def this(identifier: Identifier) {
    this(identifier.fullRunningID, identifier.id)
  }

  /**
   * Mockup for start getting events from the command topic and send them to the passed listener.
   *
   * @param  commandListener The listener of commands that execute all the commands
   * @param closeable        The closeable class to free the resources while exiting/restating
   */
  override def start(commandListener: CommandListener, closeable: AutoCloseable): Unit = {
    CommandManagerMock.logger.info("Started")
  }

  /**
   * Close the producer and the consumer and release all the allocated resources.
   */
  override def close(): Unit = {
    CommandManagerMock.logger.info("Closed")
  }
}

/** Companion object */
object CommandManagerMock {
  /** The logger */
  val logger: Logger = IASLogger.getLogger(CommandManagerMock.getClass)
}
