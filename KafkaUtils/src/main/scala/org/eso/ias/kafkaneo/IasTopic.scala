package org.eso.ias.kafkaneo

/** The topics for the IAS */
enum IasTopic(val kafkaTopicName: String) extends Enum[IasTopic] {

  /** The topic where plugins pushes values */
  case Plugins extends IasTopic("PluginsKTopic")

  /** The topic where converters and DASUs push the IASIOs */
  case Core extends IasTopic("BsdbCoreKTopic")

  /** The topic where tools publish the heartbeat */
  case Heartbeat extends IasTopic("HeartbeatTopic")

  /** The topic for commands */
  case Command extends IasTopic("CmdTopic")

  /** The topic for replies to commands */
  case Reply extends IasTopic("ReplyTopic")

  /** A topic for testing purposes */
  case Test extends IasTopic("TestTopic")

}

object IasTopic {

  /**
   * Get and return the IasTopic that corresponds to the
   * passed kafka topic name
   *
   * @param name The name of the kafka topic
   * @return the IasTopic that corresponds to the passed name
   */
  def fromKafkaTopicName(name: String): Option[IasTopic] = {
    require(!name.isBlank, "Invalid topic name")
    name match {
      case Plugins.kafkaTopicName => Some(Plugins)
      case Core.kafkaTopicName => Some(Core)
      case Heartbeat.kafkaTopicName => Some(Heartbeat)
      case Command.kafkaTopicName => Some(Command)
      case Reply.kafkaTopicName => Some(Reply)
      case Test.kafkaTopicName => Some(Test)
      case _ => Option.empty
    }

  }
}
