package org.eso.ias.kafkaneo

/**
 * Kafka consumer to get events from the BSDB.
 *
 * This consumer allows to get events from several topics: compared to
 * consumers in the kafkautils package, a process can instantiate only one consumer
 * to get events for several topics (for example IASIOS and commands).
 *
 * '''Limitations''':
 *  - the consumer uses only one group.id as it belongs to only one group; this is subject to kafka strategy
 *    on assigning consumers to topic partitions depending on the group (and other consumers in the same
 *    group).
 *    In short if you want your consumer to get all events from topic 1 and all events from topic 2
 *    then you have to ensure that the consumer is the only one consumer in the group.
 *  - the serializer/deserializer is the same for all the topics so it is the client that gets the event
 *    that has to translate the events accordingly
 *
 * @todo add the number of processed record per topic
 *
 * @constructor create a new kafka consumer
 * @param groupId the group.id property for the kafka consumer
 * @param id the id of the consumer for the kafka consumer
 * @param kafkaServers the string of servers and ports to connect to kafka servers
 * @param keyDeserializer the name of the class to deserialize the key
 * @param valueDeserializer the name of the class to deserialize the value
 * @param kafkaProperties Other kafka properties to allow the user to pass custom properties
 * @author Alessandro Caproni
 * @since 13.0
 */
class Consumer(
                val groupId: String,
                val id: String,
                val kafkaServers: String,
                val keyDeserializer: String = ConsumerHelper.DefaultKeyDeserializer,
                val valueDeserializer: String = ConsumerHelper.DefaultValueDeserializer,
                val kafkaProperties: Map[String, String]) {
}
