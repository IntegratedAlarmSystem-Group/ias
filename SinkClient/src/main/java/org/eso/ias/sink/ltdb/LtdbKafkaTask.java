package org.eso.ias.sink.ltdb;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * The task run by the kafka connector for the LTDB.
 *
 * It saves IASIOs in the Cassandra database.
 *
 * @author acaproni
 */
public class LtdbKafkaTask extends SinkTask {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(LtdbKafkaTask.class);

    @Override
    public String version() {
        return getClass().getSimpleName();
    }

    @Override
    public void start(Map<String, String> map) {
        LtdbKafkaTask.logger.info("Started");
    }

    @Override
    public void put(Collection<SinkRecord> collection) {
        LtdbKafkaTask.logger.info("Received {} records",collection.size());
    }

    @Override
    public void stop() {
        LtdbKafkaTask.logger.info("Stopped");
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
    }
}
