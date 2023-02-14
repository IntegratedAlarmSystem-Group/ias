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

}
