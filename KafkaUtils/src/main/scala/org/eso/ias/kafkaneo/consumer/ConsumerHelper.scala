package org.eso.ias.kafkaneo.consumer

import scala.jdk.javaapi.CollectionConverters

/** The position to start reading messages when a partition is assigned to a consumer */
enum StartReadingPos extends Enum[StartReadingPos] {
  case Begin // Start  reading from the beginning
  case End // Start reading from the end
  case Default // Start reading position set in the configuration (i.e. nothing done by the consumer)
}

/** Helper for handling Kafka Consumer */
object ConsumerHelper {

  /** The name of the default class to deserialize the value */
  val DefaultValueDeserializer: String = "org.apache.kafka.common.serialization.StringDeserializer"

  /** The name of the default class to deserialize the value */
  val DefaultKeyDeserializer: String = "org.apache.kafka.common.serialization.StringDeserializer"

  /** The default kafka broker */
  val DefaultKafkaBroker = "localhost:9092"


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
   * @param groupId the group.id of the consumer
   * @param userProps User defined properties
   * @param kafkaServers the kafka servers to connect to
   * @param keyDeserializer the deserializer for the keys
   * @param valueDeserializer the deserializer for the values
   * @return the properties to pass to the kafka Consumer
   */
  def setupPropsJ(
                 groupId: String,
                 userProps: java.util.Properties,
                 kafkaServers: String,
                 keyDeserializer: String,
                 valueDeserializer: String): java.util.Properties = {

    setupProps(
      groupId,
      CollectionConverters.asScala(userProps).toMap,
      kafkaServers,
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
   * @param groupId the group.id of the consumer
   * @param userProps User defined properties
   * @param kafkaServers the kafka servers to connect to
   * @param keyDeserializer the deserializer for the keys
   * @param valueDeserializer the deserializer for the values
   * @return the properties to pass to the kafka Consumer
   */
  def setupProps(
                  groupId: String,
                  userProps: Map[String, String] = Map.empty,
                  kafkaServers: String = DefaultKafkaBroker,
                  keyDeserializer: String = DefaultKeyDeserializer,
                  valueDeserializer: String = DefaultValueDeserializer): java.util.Properties = {

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
