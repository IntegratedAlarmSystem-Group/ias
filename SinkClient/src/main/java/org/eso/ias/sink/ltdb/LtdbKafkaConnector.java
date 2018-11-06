package org.eso.ias.sink.ltdb;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The Kafka connector for the LTDB
 *
 * @author acaproni
 */
public class LtdbKafkaConnector extends SinkConnector {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(LtdbKafkaConnector.class);

    /**
     * The name of the property to pass the cassandra contact points
     */
    public static final String CASSANDRA_CONTACT_POINTS_PROPNAME = "cassandra.contact.points";

    /**
     * The name of the property to pass the cassandra keyspace
     */
    public static final String CASSANDRA_KEYSPACE_PROPNAME = "cassandra.keyspace";

    /**
     * The properties to pass to the task
     */
    private final Map<String, String> propsForTask = new HashMap<>();

    @Override
    public void start(Map<String, String> map) {
        LtdbKafkaConnector.logger.info("Started");

        String contactPoints = map.get(CASSANDRA_CONTACT_POINTS_PROPNAME);
        String keyspace= map.get(CASSANDRA_KEYSPACE_PROPNAME);

        LtdbKafkaConnector.logger.info("Cassandra contact points: {}",contactPoints);
        LtdbKafkaConnector.logger.info("Cassandra keyspace: {}",keyspace);

        propsForTask.put(CASSANDRA_CONTACT_POINTS_PROPNAME,contactPoints);
        propsForTask.put(CASSANDRA_KEYSPACE_PROPNAME,keyspace);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return LtdbKafkaTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int i) {
        ArrayList<Map<String, String>> configs = new ArrayList<>();
        // Only one input stream makes sense.
        configs.add(propsForTask);
        return configs;
    }

    @Override
    public void stop() {
        LtdbKafkaConnector.logger.info("Stopped");
    }

    @Override
    public ConfigDef config() {
        ConfigDef ret =  new ConfigDef();

        ret.define(CASSANDRA_CONTACT_POINTS_PROPNAME, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,"Cassandra contact points");
        ret.define(CASSANDRA_KEYSPACE_PROPNAME, ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,"LTDB keyspace");
        return ret;
    }

    @Override
    public String version() {
        return getClass().getSimpleName();
    }
}
