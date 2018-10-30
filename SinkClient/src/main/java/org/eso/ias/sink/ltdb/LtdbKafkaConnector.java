package org.eso.ias.sink.ltdb;

import org.apache.commons.cli.*;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.IasCdbException;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.rdb.RdbReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

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

    @Override
    public void start(Map<String, String> map) {
        LtdbKafkaConnector.logger.info("Started");
    }

    @Override
    public Class<? extends Task> taskClass() {
        return LtdbKafkaTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int i) {
        ArrayList<Map<String, String>> configs = new ArrayList<>();
        // Only one input stream makes sense.
        Map<String, String> config = new HashMap<>();
        configs.add(config);
        return configs;
    }

    @Override
    public void stop() {
        LtdbKafkaConnector.logger.info("Stopped");
    }

    @Override
    public ConfigDef config() {
        ConfigDef ret =  new ConfigDef();

        ret.define("cassandra.contact.points", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,"Cassandra contact points");
        ret.define("ltdb.keyspace", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH,"LTDB keyspace");
        return ret;
    }

    @Override
    public String version() {
        return getClass().getSimpleName();
    }
}
