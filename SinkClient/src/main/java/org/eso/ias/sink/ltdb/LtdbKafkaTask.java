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
import java.util.concurrent.atomic.AtomicLong;

import static org.eso.ias.sink.ltdb.LtdbKafkaConnector.*;

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
     * The time interval (msecs) to log statistics
     */
    private long statsTimeInterval=0;

    /**
     * The last point in time when statistics have been logged
     */
    private final AtomicLong lastStatGenerationTime = new AtomicLong(System.currentTimeMillis());

    /**
     * The number of IASIOs read from the BSDB and stored in the LTDB in the last time interval
     */
    private final AtomicLong messagesProcessedInTheLastTimeIneterval = new AtomicLong(0);

    /**
     * The max size of the buffer in the last time interval
     */
    private final AtomicLong maxBufferSizeInTheLastTimeInterval = new AtomicLong(0);

    /**
     * The max size of the buffer since beginning of execution
     */
    private final AtomicLong maxBufferSizeSinceEver = new AtomicLong(0);

    /**
     * The max time to flush the buffer (msec) in the past time interval
     */
    private final AtomicLong maxBufferFlushTimeInTheLastTimeInterval = new AtomicLong(0);

    /**
     * The max time to flush the buffer since beginning of execution
     */
    private final AtomicLong maxBufferFlushTimeSinceEver = new AtomicLong(0);

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
        String statsTI = map.getOrDefault(CASSANDRA_STATS_TIME_INTERVAL_PROPNAME,"0");

        long statsTimeIntervalMinutes = Long.parseLong(statsTI);
        statsTimeInterval = TimeUnit.MILLISECONDS.convert(statsTimeIntervalMinutes,TimeUnit.MINUTES);

        LtdbKafkaTask.logger.info("Cassandra contact points: {}",contactPoints);
        LtdbKafkaTask.logger.info("Cassandra keyspace: {}",keyspace);
        LtdbKafkaTask.logger.info("Cassandra TTL: {}",ttl);

        if (statsTimeIntervalMinutes>0) {
           LtdbKafkaTask.logger.info("Will log statistics every {} minutes",statsTimeIntervalMinutes);
        } else {
            LtdbKafkaTask.logger.info("Will NOT log statistics");
        }

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

            // Update stats
            maxBufferSizeInTheLastTimeInterval.set(Long.max(maxBufferSizeInTheLastTimeInterval.get(),buffer.size()));
            maxBufferSizeSinceEver.set(Long.max(maxBufferSizeSinceEver.get(),buffer.size()));
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
        LtdbKafkaTask.logger.debug("Flushing {} items",buffer.size());

        Collection<String> jsonStrings = new Vector<>();
        int numOfElementsToStore=buffer.drainTo(jsonStrings);

        long before = System.currentTimeMillis();
        cassandraHelper.store(jsonStrings);
        long storeTime = System.currentTimeMillis()-before;

        super.flush(currentOffsets);
        LtdbKafkaTask.logger.debug("Buffer flushed and items stored in the LTDB in {} msecs",storeTime);

        // Update stats
        messagesProcessedInTheLastTimeIneterval.addAndGet(numOfElementsToStore);
        maxBufferFlushTimeInTheLastTimeInterval.set(Long.max(maxBufferFlushTimeInTheLastTimeInterval.get(),storeTime));
        maxBufferFlushTimeSinceEver.set(Long.max(maxBufferFlushTimeSinceEver.get(),storeTime));
    }

    /**
     * Log statistics
     */
    private void logStats() {
        long timeInterval = TimeUnit.MINUTES.convert(statsTimeInterval,TimeUnit.MILLISECONDS);
        LtdbKafkaTask.logger.info("Stats: {} IASIOs processed in the past {} minutes ({} per minute)",
            messagesProcessedInTheLastTimeIneterval.get(),
            messagesProcessedInTheLastTimeIneterval.getAndSet(0)/timeInterval,
            timeInterval);
        LtdbKafkaTask.logger.info("Stats: {} effectively stored in the LTDB ({} errors and IASIOs lost) in the past {} minutes",
            cassandraHelper.getValuesStored(true),
            cassandraHelper.getErrors(true),
            timeInterval);
        LtdbKafkaTask.logger.info("Stats: max buffer size {} ({} in the past {} minutes)",
            maxBufferSizeSinceEver.get(),
            maxBufferSizeInTheLastTimeInterval.getAndSet(0),
            timeInterval);
        LtdbKafkaTask.logger.info("Stats: max time to flush the buffer {} msecs ({} msecs in the past {} minutes)",
            maxBufferFlushTimeSinceEver.get(),
            maxBufferFlushTimeInTheLastTimeInterval.getAndSet(0),
            timeInterval);
        lastStatGenerationTime.set(System.currentTimeMillis());
    }

    /**
     * The thread that store elements in the LTDB before the flush is called.
     *
     * The thread logs statistics if the time interval elapsed.
     */
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
                // Update stats
                messagesProcessedInTheLastTimeIneterval.incrementAndGet();
            }
            if (statsTimeInterval>0 && System.currentTimeMillis()>lastStatGenerationTime.get()+statsTimeInterval) {
                logStats();
            }
        }
    }
}
