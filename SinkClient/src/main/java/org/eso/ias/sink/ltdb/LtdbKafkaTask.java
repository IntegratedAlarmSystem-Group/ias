package org.eso.ias.sink.ltdb;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.eso.ias.sink.ltdb.LtdbKafkaConnector.CASSANDRA_CONTACT_POINTS_PROPNAME;
import static org.eso.ias.sink.ltdb.LtdbKafkaConnector.CASSANDRA_KEYSPACE_PROPNAME;
import static org.eso.ias.sink.ltdb.LtdbKafkaConnector.CASSANDRA_TTL_PROPNAME;

/**
 * The task run by the kafka connector for the LTDB.
 *
 * It saves IASIOs in the Cassandra database.
 *
 * No need to synchronize because Kafka connectors run on a
 * single thread.
 *
 * Received IASIOs are saved in the buffer to quickly return from the
 * put method. Asynchronously the thread stores IASIOs in the LTDB.
 * When the flush is executed all the remainings IASIOs in the buffer
 * are saved in the LTDB.
 *
 * @author acaproni
 */
public class LtdbKafkaTask extends SinkTask implements Runnable {

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(LtdbKafkaTask.class);

    /**
     * The buffer of JSON strings representing IASIOs read from the BSDB.
     *
     * All received IASIOs are stored in this list when received and sent to the
     * LTDB by the thread.
     */
    private final LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>();

    /**
     * The Thread that executes this runnable
     */
    private Thread thread;

    /**
     * Signal teh thread to terminate
     */
    private volatile boolean terminateThread=false;

    /**
     * The helper to store IASValues in the Cassandra LTDB
     */
    private CassandraHelper cassandraHelper = new CassandraHelper();

    @Override
    public String version() {
        return getClass().getSimpleName();
    }

    @Override
    public void start(Map<String, String> map) {
        String contactPoints = map.get(CASSANDRA_CONTACT_POINTS_PROPNAME);
        String keyspace= map.get(CASSANDRA_KEYSPACE_PROPNAME);
        String ttl = map.getOrDefault(CASSANDRA_TTL_PROPNAME,"0");

        LtdbKafkaTask.logger.info("Cassandra contact points: {}",contactPoints);
        LtdbKafkaTask.logger.info("Cassandra keyspace: {}",keyspace);
        LtdbKafkaTask.logger.info("Cassandra TTL: {}",ttl);

        cassandraHelper.start(contactPoints,keyspace,Long.valueOf(ttl));

        thread = new Thread(this,"LtdbKafkaTask-thread");
        thread.setDaemon(true);
        thread.start();
        LtdbKafkaTask.logger.info("Started");
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        LtdbKafkaTask.logger.debug("Received {} records",records.size());
        for (SinkRecord record: records) {
            String jsonStr=record.value().toString();
            buffer.offer(jsonStr);
        }
    }

    @Override
    public  void stop() {
        LtdbKafkaTask.logger.info("Stopped");
        terminateThread=true;
        if (thread!=null) {
            thread.interrupt();
        }

        cassandraHelper.stop();
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
        LtdbKafkaTask.logger.info("Flushing {} items",buffer.size());

        Collection<String> jsonStrings = new Vector<>();
        buffer.drainTo(jsonStrings);
        cassandraHelper.store(jsonStrings);

        super.flush(currentOffsets);
    }

    @Override
    public void run() {
        while (!terminateThread) {
            String jStr;
            try {
                jStr = buffer.poll(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                continue;
            }
            if (jStr!=null) {
                cassandraHelper.store(jStr);
            }
        }
    }
}
