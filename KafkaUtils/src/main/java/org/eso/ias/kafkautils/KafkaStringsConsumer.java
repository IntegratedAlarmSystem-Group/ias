package org.eso.ias.kafkautils;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A generic kafka consumer that forwards all the strings received in
 * each message to the listener.
 *
 * Kafka optimize the polling by returning a bounch of messages to each poll:
 * objects of this class returns all the records received in a poll to the listener.
 * If the listener wants to process one message at a time, should use {@link SimpleStringConsumer}
 * instead.
 *
 * {@link KafkaStringsConsumer} runs the next poll after the listener terminates processing the
 * records.
 *
 * <P>
 * Kafka properties are fully customizable by calling {@link #setUp(Properties)}:
 * defaults values are used for the missing ones.
 * <P>
 * <EM>Life cycle</em>: {@link #setUp()} or {@link #setUp(Properties)}
 *                         must be called to initialize the object;
 *                         {@link #tearDown()} must be called when finished using the object;
 *                         {@link #startGettingEvents(StringsConsumer, StartPosition)} must be called to start
 *                         polling events from the kafka topic
 *
 * {@link #startGettingEvents(StringsConsumer, StartPosition)} returns when the consumer has been assigned to
 * at least one partition. There are situations when the partitions assigned to the consumer
 * can be revoked and reassigned like for example when another consumer subscribe or disconnect
 * as the assignment of consumers to partitions is left to kafka in this version.
 *
 *
 * @author acaproni
 */
public class KafkaStringsConsumer implements Runnable {

    /**
     * The interface fopr the listener of strings
     */
    public interface StringsConsumer {
        /**
         * Sends the strings received from the kafka topic
         * to the listener
         */
        void stringsReceived(Collection<String> strings);
    }

    /**
     * The start position when connecting to a kafka topic
     * for reading strings
     *
     * @author acaproni
     */
    public enum StartPosition {
        // The default position, usually set in kafka configurations
        DEFAULT,
        // Get events from the beginning of the partition
        BEGINNING,
        // Get events from the end of the partition
        END
    }

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(org.eso.ias.kafkautils.KafkaStringsConsumer.class);

    /**
     * The name of the topic to get events from
     */
    private final String topicName;

    /**
     * The ID of the consumer
     *
     * Needed for writing logs as it can happen to have more then
     * one consumer logging messages
     */
    private final String consumerID;

    /**
     * The thread to cleanly close the consumer
     */
    private Thread shutDownThread;

    /**
     * The servers of the kafka broker runs
     */
    private final String kafkaServers;

    /**
     * The time, in milliseconds, spent waiting in poll if data is not available in the buffer
     */
    private static final Duration POLLING_TIMEOUT = Duration.ofSeconds(15);

    /**
     * The consumer getting events from the kafka topic
     */
    private KafkaConsumer<String, String> consumer;

    /**
     * The boolean set to <code>true</code> when the
     * consumer has been closed
     */
    private AtomicBoolean isClosed=new AtomicBoolean(false);

    /**
     * A flag signaling that the shutdown is in progress
     */
    private AtomicBoolean isShuttingDown=new AtomicBoolean(false);

    /**
     * The boolean set to <code>true</code> when the
     * consumer has been initialized
     */
    private volatile AtomicBoolean isInitialized=new AtomicBoolean(false);

    /**
     * The thread getting data from the topic
     */
    private final AtomicReference<Thread> thread = new AtomicReference<>(null);

    /**
	 * The listener of events published in the topic
	 */
	private StringsConsumer stringsListener;

    /**
     * The number of records received while polling
     */
    private final AtomicLong processedRecords = new AtomicLong(0);

    /**
     * The position to start reading from
     */
    private StartPosition startReadingPos = StartPosition.DEFAULT;

    /**
     * Max time to wait for the assignement of partitions before polling
     * (in minutes)
     */
    private static final int WAIT_FOR_PARTITIONS_TIMEOUT = 3;

    /**
     * The latch to wait until the consumer has been initialized and
     * is effectively polling for events
     */
    private final CountDownLatch polling = new CountDownLatch(1);

    /**
     * Signal that a partition has been assigned: seeking is done onlyt if a partition
     * is assigned
     */
    private final AtomicBoolean isPartitionAssigned = new AtomicBoolean(false);

    /**
     * @return the number of records processed
     */
    public long getNumOfProcessedRecords() {
        return processedRecords.get();
    }

    /**
     * Constructor
     *
     * @param servers The kafka servers to connect to
     * @param topicName The name of the topic to get events from
     * @param consumerID the ID of the consumer
     */
    public KafkaStringsConsumer(String servers, String topicName, String consumerID) {
        Objects.requireNonNull(servers);
        this.kafkaServers = servers;
        Objects.requireNonNull(topicName);
        if (topicName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid empty topic name");
        }
        this.topicName=topicName.trim();
        Objects.requireNonNull(consumerID);
        if (consumerID.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid empty consumer ID");
        }
        this.consumerID=consumerID.trim();
        KafkaStringsConsumer.logger.info("The consumer [{}] will get events from {} topic connected to kafka broker@{}",
                consumerID,
                this.topicName,
                this.kafkaServers);
    }

    /**
     * Initializes the consumer with the passed kafka properties.
     * <P>
     * The defaults are used if not found in the parameter
     *
     * @param userPros The user defined kafka properties
     */
    public synchronized void setUp(Properties userPros) {
        Objects.requireNonNull(userPros);
        if (isInitialized.get()) {
            throw new IllegalStateException("Already initialized");
        }
        mergeDefaultProps(userPros);
        consumer = new KafkaConsumer<>(userPros);

        shutDownThread = new Thread( () -> {
            isShuttingDown.set(true);
            tearDown();
        });

        Runtime.getRuntime().addShutdownHook(shutDownThread);

        isInitialized.set(true);
        KafkaStringsConsumer.logger.info("Kafka consumer [{}] initialized",consumerID);
    }

    /**
     * Start polling events from the kafka channel.
     * <P>
     * This method starts the thread that polls the kafka topic
     * and returns after the consumer has been assigned to at least
     * one partition.
     *
     * @param listener The listener of events published in the topic
     * @param startReadingFrom Starting position in the kafka partition
     * @throws KafkaUtilsException in case of timeout subscribing to the kafkatopic
     */
    public synchronized void startGettingEvents(
            StringsConsumer listener,
            StartPosition startReadingFrom)
            throws KafkaUtilsException {
        Objects.requireNonNull(startReadingFrom);
        Objects.requireNonNull(listener);
        this.stringsListener=listener;
        KafkaStringsConsumer.logger.info("Preparing to consume strings");
        if (!isInitialized.get()) {
            throw new IllegalStateException("Not initialized");
        }
        if (thread.get()!=null) {
            KafkaStringsConsumer.logger.warn("Consumer [{}] cannot start receiving: already receiving events!",consumerID);
            return;
        }
        startReadingPos = startReadingFrom;
        Thread getterThread = new Thread(this, KafkaStringsConsumer.class.getName() + "-Thread");
        getterThread.setDaemon(true);
        thread.set(getterThread);
        getterThread.start();

        try {
            KafkaStringsConsumer.logger.debug("Waiting for kafka partition assignment...");
            if (!polling.await(WAIT_FOR_PARTITIONS_TIMEOUT, TimeUnit.MINUTES)) {
                throw new KafkaUtilsException("Timed out while waiting for assignmnt to kafka partitions");
            }
            KafkaStringsConsumer.logger.debug("Kafka partition assigned");
        } catch (InterruptedException e) {
            KafkaStringsConsumer.logger.warn("Consumer [{}] Interrupted",consumerID);
            Thread.currentThread().interrupt();
            throw new KafkaUtilsException(consumerID+" interrupted while waiting for assignemnt to kafka partitions", e);
        }
        KafkaStringsConsumer.logger.info("Is consuming strings");
    }

    /**
     * Stop getting events from the kafka topic.
     * <P>
     * The user cannot stop the getting of the events because this is
     * part of the shutdown.
     */
    private synchronized void stopGettingEvents() {
        if (thread.get()==null) {
            KafkaStringsConsumer.logger.error("[{}] cannot stop receiving events as I am not receiving events!",consumerID);
            return;
        }
        consumer.wakeup();
        try {
            thread.get().join(60000);
            if (thread.get().isAlive()) {
                KafkaStringsConsumer.logger.error("The thread of [{}] to get events did not exit",consumerID);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        thread.set(null);
    }

    /**
     * Merge the default properties into the passed properties.
     *
     * @param props The properties where missing ones are taken from the default
     */
    private void mergeDefaultProps(Properties props) {
        Objects.requireNonNull(props);

        Properties defaultProps = getDefaultProps();
        defaultProps.keySet().forEach( k -> props.putIfAbsent(k, defaultProps.get(k)));
    }

    /**
     * Build and return the default properties for the consumer
     * @return the default properites
     */
    private Properties getDefaultProps() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaServers);
        props.put("group.id", KafkaHelper.DEFAULT_CONSUMER_GROUP);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "latest");
        return props;
    }

    /**
     * Initializes the consumer with default kafka properties
     */
    public void setUp() {
        KafkaStringsConsumer.logger.info("Setting up the kafka consumer [{}]",consumerID);

        setUp(getDefaultProps());
    }

    /**
     * Close and cleanup the consumer
     */
    public synchronized void tearDown() {
        if (isClosed.get()) {
            KafkaStringsConsumer.logger.warn("Consumer [{}] already closed",consumerID);
            return;
        }
        KafkaStringsConsumer.logger.debug("Closing consumer [{}]...",consumerID);
        isClosed.set(true);
        if (!isShuttingDown.get()) {
            Runtime.getRuntime().removeShutdownHook(shutDownThread);
        }
        stopGettingEvents();
        KafkaStringsConsumer.logger.info("Consumer [{}] cleaned up",consumerID);
    }

    /**
     * The thread to get data out of the topic
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        KafkaStringsConsumer.logger.info("Thread of consumer [{}] to get events from the topic {} started",consumerID,topicName);

        if (startReadingPos==StartPosition.DEFAULT) {
            consumer.subscribe(Arrays.asList(topicName));
        } else {
            consumer.subscribe(Arrays.asList(topicName), new ConsumerRebalanceListener() {

                /**
                 * Returns a string of topics and partitions contained in the
                 * passed collection
                 *
                 * @param parts The partitions to generate the string from
                 * @return a string of topic:partition
                 */
                private String formatPartitionsStr(Collection<TopicPartition> parts) {
                    StringBuilder partitions = new StringBuilder();
                    for (TopicPartition topicPartition: parts ) {
                        partitions.append(topicPartition.topic());
                        partitions.append(':');
                        partitions.append(topicPartition.partition());
                        partitions.append(' ');
                    }
                    return partitions.toString();
                }

                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> parts) {
                    isPartitionAssigned.set(false);
                    KafkaStringsConsumer.logger.info("Partition(s) of consumer [{}] revoked: {}",consumerID, formatPartitionsStr(parts));

                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> parts) {
                    isPartitionAssigned.set(false);
                    KafkaStringsConsumer.logger.info("Consumer [{}] assigned to {} partition(s): {}",
                            consumerID,
                            parts.size(),
                            formatPartitionsStr(parts));
                    if (startReadingPos==StartPosition.BEGINNING) {
                        consumer.seekToBeginning(new ArrayList<>());
                    } else {
                        consumer.seekToEnd(new ArrayList<>());
                    }
                    polling.countDown();
                }
            });
        }

        KafkaStringsConsumer.logger.debug("Consumer [{}] : start polling loop",consumerID);
        while (!isClosed.get()) {
            ConsumerRecords<String, String> records;
            try {
                records = consumer.poll(POLLING_TIMEOUT);
                KafkaStringsConsumer.logger.debug("Consumer [{}] got an event read with {} records", consumerID, records.count());
                processedRecords.incrementAndGet();
            } catch (WakeupException we) {
                KafkaStringsConsumer.logger.warn("Consumer [{}]: no values read from the topic {} in the past {} seconds",
                        consumerID,
                        topicName,
                        POLLING_TIMEOUT.getSeconds());
                continue;
            }

            notifyListener(records);
        }
        KafkaStringsConsumer.logger.debug("Closing the consumer [{}]",consumerID);
        consumer.close();
        KafkaStringsConsumer.logger.info("Thread of [{}] to get events from the topic terminated",consumerID);
    }

    /**
     * Sends the strings read from the Kafka record to the listener
     *
     * @param records The records read from the topic
     */
    private void notifyListener(ConsumerRecords<String, String> records) {
        if (records!=null && !records.isEmpty()) {
            Collection<String> ret = new ArrayList<>();
            // Get the strings from the vector
            for (ConsumerRecord<String, String> record: records) {
                String value = record.value();
                if (value!=null && !value.isEmpty()) {
                    ret.add(value);
                }
            }
            // Notify the listener
            try {
                KafkaStringsConsumer.logger.debug("Notify the consumer of {} records",ret.size());
                stringsListener.stringsReceived(ret);
            } catch (Exception e) {
                KafkaStringsConsumer.logger.error("Consumer [{}] got an exception got processing events: records lost!",consumerID,e);
            } finally {
                ret.clear();
            }
        }
    }

    /**
     * Seek the consumer to the passed postion.
     * Kafka allows to seek only if a partition is assigned
     * otherwise the seek is ignored.
     *
     * @param pos The position to seek the consumer
     * @return true is the seek has been done with an asigned partiton;
     *         false otherwise
     */
    public boolean seekTo(StartPosition pos) {
        Objects.requireNonNull(pos,"Invalid position given");
        if (!isClosed.get() || !isPartitionAssigned.get()) {
            if (pos==StartPosition.BEGINNING) {
                consumer.seekToBeginning(new ArrayList<>());
            } else {
                consumer.seekToEnd(new ArrayList<>());
            }
            return true;
        } else {
            return false;
        }
    }
}
