package org.eso.ias.kafkaneo.consumer

import scala.jdk.javaapi.CollectionConverters

/** Helper for handling Kafka Consumer */
object ConsumerHelper {

  /** The name of the default class to deserialize the value */
  val DefaultValueDeserializer: String = "org.apache.kafka.common.serialization.StringDeserializer"

  /** The name of the default class to deserialize the value */
  val DefaultKeyDeserializer: String = "org.apache.kafka.common.serialization.StringDeserializer"


  /** Default properties for the consumer */
  val DefaultProps = Map(
    "enable.auto.commit" -> "true",
    "auto.commit.interval.ms" -> "1000",
    "auto.offset.reset" -> "latest")

  /**
   * Prepare the properties for the kafka consumer for java.
   *
   * Delegates to [[setupProps()]].
   *
   * @param userProps User defined properties
   * @param kafkaServers the kafka servers to connect to
   * @param groupId the group.id of the consumer
   * @param keyDeserializer the deserializer for the keys
   * @param valueDeserializer the deserializer for the values
   * @return the properties to pass to the kafka Consumer
   */
  def setupPropsJ(
                  userProps: java.util.Properties,
                  kafkaServers: String,
                  groupId: String,
                  keyDeserializer: String,
                  valueDeserializer: String): java.util.Properties = {

    setupProps(
      CollectionConverters.asScala(userProps).toMap,
      kafkaServers,
      groupId,
      keyDeserializer,
      valueDeserializer)
  }

  /**
   * Prepare the properties for the kafka consumer.
   *
   * The properties are taken from (whatever comes first):
   *  - java properties in the command line (-Dauto.commit.interval.ms=...)
   *  - the user properties passed to this function
   *  - the parameters of this function
   *  - default value (if exists)
   *
   * Default properties ([[DefaultProps]]) are added if not already present in the
   * user.properties
   *
   * @param userProps User defined properties
   * @param kafkaServers the kafka servers to connect to
   * @param groupId the group.id of the consumer
   * @param keyDeserializer the deserializer for the keys
   * @param valueDeserializer the deserializer for the values
   * @return the properties to pass to the kafka Consumer
   */
  def setupProps(
                  userProps: Map[String, String],
                  kafkaServers: String,
                  groupId: String,
                  keyDeserializer: String,
                  valueDeserializer: String): java.util.Properties = {

    val systemProps: Map[String, String] = CollectionConverters.asScala(System.getProperties).toMap

    val ret = new java.util.Properties()
    userProps.foreach(pair => ret.setProperty(pair._1, pair._2))

    ret.setProperty("group.id", systemProps.getOrElse("group.id", userProps.getOrElse("group.id", groupId)))
    ret.setProperty("bootstrap.servers", systemProps.getOrElse("bootstrap.servers", userProps.getOrElse("bootstrap.servers", kafkaServers)))
    ret.setProperty("key.deserializer", systemProps.getOrElse("key.deserializer", userProps.getOrElse("key.deserializer", keyDeserializer)))
    ret.setProperty("value.deserializer", systemProps.getOrElse("value.deserializer", userProps.getOrElse("value.deserializer", valueDeserializer)))

    // Set the default values for the properties if not not yet set
    DefaultProps.keys.foreach(k => if !ret.contains(k) then ret.setProperty(k, DefaultProps(k)))

    ret
  }
}
