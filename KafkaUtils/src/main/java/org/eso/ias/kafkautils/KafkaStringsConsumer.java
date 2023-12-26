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
 *                         {@link #startGettingEvents(StringsConsumer, StreamPosition)} must be called to start
 *                         polling events from the kafka topic
 *
 * {@link #startGettingEvents(StringsConsumer, StreamPosition)} returns when the consumer has been assigned to
 * at least one partition. There are situations when the partitions assigned to the consumer
 * can be revoked and reassigned like for example when another consumer subscribe or disconnect
 * as the assignment of consumers to partitions is left to kafka in this version.
 *
 *
 * @author acaproni
 */
public class KafkaStringsConsumer implements Runnable, ConsumerRebalanceListener {

    /**
     * The interface for the listener of strings
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
    public enum StreamPosition {
        // The default position, usually set in kafka configurations
        DEFAULT,
        // Get events from the beginning of the partition
        BEGIN,
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
    protected final String topicName;

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
     * The property to setup the waiting time until the consumer is ready (in msecs)
     */
    public static final String WAIT_CONSUMER_READY_TIMEOUT_PROP_NAME = "org.eso.ias.kafka.consumerready.timeout";

    /**
     * Default time to wait until the consumer is ready
     */
    public static final long DEFAULT_CONSUMER_READY_TIMEOUT = 15000;

    /**
     * Min allowed timeout to wait for the consumer ready
     */
    public static final long MIN_CONSUMER_READY_TIMEOUT = 5000;

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
     * The boolean set to <code>true</code> when the
     * consumer is polling events
     */
    private volatile AtomicBoolean isGettingEvents=new AtomicBoolean(false);

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
    private StreamPosition startReadingPos = StreamPosition.DEFAULT;

    /**
     * Signal that a partition has been assigned: seeking is done only if a partition
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
            StreamPosition startReadingFrom)
            throws KafkaUtilsException {
        Objects.requireNonNull(startReadingFrom);
        Objects.requireNonNull(listener);
        boolean alreadyGettingEvents = isGettingEvents.getAndSet(true);
        if (alreadyGettingEvents) {
            KafkaStringsConsumer.logger.warn("Consumer [{}] is already getting events from topic {}", consumerID, topicName);
            return;
        }
        this.stringsListener=listener;
        KafkaStringsConsumer.logger.info("Consumer [{}] prepares to consume strings from topic {}", consumerID, topicName);
        if (!isInitialized.get()) {
            throw new IllegalStateException("Not initialized");
        }
        if (thread.get()!=null) {
            KafkaStringsConsumer.logger.warn("Consumer [{}] cannot start receiving: already receiving events!",consumerID);
            return;
        }

        // Subscribe the consumer to the topic
        KafkaStringsConsumer.logger.debug("Consumer [{}]: subscribing to topic {}", consumerID, topicName);
        consumer.subscribe(Arrays.asList(topicName), this);
        KafkaStringsConsumer.logger.info("Consumer [{}]: subscribed to topic {}", consumerID, topicName);

        // Start the thread to poll events from the topic
        startReadingPos = startReadingFrom;
        Thread getterThread = new Thread(this, KafkaStringsConsumer.class.getName() + "-Thread");
        getterThread.setDaemon(true);
        thread.set(getterThread);
        getterThread.start();

        waitUntilConsumerReady();
    }

    /**
     * Stop getting events from the kafka topic.
     * <P>
     * The user cannot stop the getting of the events because this is
     * part of the shutdown.
     */
    private synchronized void stopGettingEvents() {
        if (thread.get()==null) {
            KafkaStringsConsumer.logger.warn("[{}] cannot stop receiving events as I am not receiving events!",consumerID);
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
        if (isClosed.getAndSet(true)) {
            KafkaStringsConsumer.logger.warn("Consumer [{}] already closed",consumerID);
            return;
        }
        KafkaStringsConsumer.logger.debug("Closing consumer [{}]...",consumerID);
        if (!isShuttingDown.get()) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutDownThread);
            } catch (IllegalStateException e) { }// Already shutting down
        }
        stopGettingEvents();
        KafkaStringsConsumer.logger.info("Consumer [{}] cleaned up",consumerID);
    }

    /**
     * Returns a string of topics and partitions contained in the
     * passed collection
     *
     * @param parts The partitions to generate the string from
     * @return a string of topic:partition
     */
    private String formatPartitionsStr(Collection<TopicPartition> parts) {
        StringBuilder partitions = new StringBuilder();
        for (TopicPartition topicPartition : parts) {
            partitions.append(topicPartition.topic());
            partitions.append(':');
            partitions.append(topicPartition.partition());
            partitions.append(' ');
        }
        return partitions.toString();
    }

    /**
     * Called before the rebalancing starts and after the consumer stopped consuming events.
     *
     * @param parts The list of partitions that were assigned to the consumer and now need to be revoked (may not
     *              include all currently assigned partitions, i.e. there may still be some partitions left)
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> parts) {
        isPartitionAssigned.set(!consumer.assignment().isEmpty());
        if (parts.isEmpty()) {
            KafkaStringsConsumer.logger.info("Consumer [{}]: no partitions need to be revoked", consumerID);
        } else {
            KafkaStringsConsumer.logger.info("Consumer [{}]: {} partition(s) need to be revoked {}",
                    consumerID,
                    parts.size(),
                    formatPartitionsStr(parts));
        }
    }

    /**
     * Called after partitions have been reassigned but before the consumer starts consuming messages
     *
     * @param parts The list of partitions that are now assigned to the consumer (previously owned partitions will
     *              NOT be included, i.e. this list will only include newly added partitions)
     */
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> parts) {
        isPartitionAssigned.set(!consumer.assignment().isEmpty());
        if (!parts.isEmpty()) {
            KafkaStringsConsumer.logger.info("Consumer [{}]: {} new partitions assigned {}",
                    consumerID,
                    parts.size(),
                    formatPartitionsStr(parts));
            if (startReadingPos == StreamPosition.BEGIN) {
                KafkaStringsConsumer.logger.debug("Consumer [{}]: seeking to the beginning", consumerID);
                consumer.seekToBeginning(new ArrayList<>());
            } else if (startReadingPos == StreamPosition.END) {
                KafkaStringsConsumer.logger.debug("Consumer [{}]: seeking to the end", consumerID);
                consumer.seekToEnd(new ArrayList<>());
            }
        } else {
            KafkaStringsConsumer.logger.info("Consumer [{}]: no new partitions assigned", consumerID);
        }
    }

    /**
     * @param parts The list of partitions that were assigned to the consumer and now have been reassigned
     *              to other consumers. With the current protocol this will always include all of the consumer's
     *              previously assigned partitions, but this may change in future protocols (ie there would still
     *              be some partitions left)
     */
    @Override
    public void onPartitionsLost(Collection<TopicPartition> parts) {
        KafkaStringsConsumer.logger.info("Consumer [{}] {} partitions reassigned to other consumers: {}",
                consumerID,
                parts.size(),
                formatPartitionsStr(parts));
        isPartitionAssigned.set(!consumer.assignment().isEmpty());
    }

    /**
     * The thread to poll data from the topic
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        KafkaStringsConsumer.logger.debug("Consumer [{}]: thread to poll events from the topic {} started",consumerID,topicName);

        KafkaStringsConsumer.logger.debug("Consumer [{}]: thread running the polling loop", consumerID);
         while (!isClosed.get()) {
            ConsumerRecords<String, String> records;
            try {
                records = consumer.poll(POLLING_TIMEOUT);
                KafkaStringsConsumer.logger.debug("Consumer [{}] got {} records from topic {}",
                        consumerID,
                        records.count(),
                        topicName);
                processedRecords.incrementAndGet();
            } catch (WakeupException we) {
                if (!isClosed.get()) { // Ignore the exception when closing
                        KafkaStringsConsumer.logger.warn("Consumer [{}]: no values read from the topic {} in the past {} seconds",
                                consumerID,
                                topicName,
                                POLLING_TIMEOUT.getSeconds());
                }
                continue;
            } catch (Throwable t) {
                KafkaStringsConsumer.logger.error("Consumer [{}] got an exception while reading events on topic {}",
                        consumerID,
                        topicName,
                        t);
                continue;
            }

            notifyListener(records);
        }
        KafkaStringsConsumer.logger.debug("Consumer [{}]: closing the kafka consumer",consumerID);
        consumer.close();
        KafkaStringsConsumer.logger.info("Consumer [{}]: thread to get events from the topic {} terminated",
                consumerID, topicName);
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
                KafkaStringsConsumer.logger.error("Consumer [{}] got an exception processing events: records lost!",consumerID,e);
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
     * @return true is the seek has been done with an assigned partiton;
     *         false otherwise
     */
    public boolean seekTo(StreamPosition pos) {
        Objects.requireNonNull(pos,"Invalid position given");
        if (!isClosed.get() && isPartitionAssigned.get()) {
            if (pos== StreamPosition.BEGIN) {
                consumer.seekToBeginning(new ArrayList<>());
            } else {
                consumer.seekToEnd(new ArrayList<>());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return true if the consumer is ready.
     *
     * Kafka API does not provide a way to know if the consumer is ready.
     * This method uses {@link KafkaConsumer#assignment()}} to understand if the
     * consumer is ready: the consumer is ready if there are partitions assigned to it.
     *
     * @return true if the consumer is ready, false otherwise
     */
    public boolean isReady() {
        return isInitialized.get() && isPartitionAssigned.get();
    }

    /**
     * Waits until the consumer is ready or a timeout elapses.
     *
     * This methods waits until the consumer is ready (delegating to {@link #isReady()})or a timeout elapses.
     * If at the end of the timeout the consumer is not yet ready, this method only logs a warning
     * as we do not want to block the process in case the consumer becomes ready after for whatever reason.
     * But still the situation is probably not normal and we want to log the event.
     *
     * The timeout can be set by setting {@link #WAIT_CONSUMER_READY_TIMEOUT_PROP_NAME} property
     * otherwise the default ({@link #DEFAULT_CONSUMER_READY_TIMEOUT}) is used.
     */
    public void waitUntilConsumerReady() {
        Properties props = System.getProperties();
        String timeoutStr = props.getProperty(WAIT_CONSUMER_READY_TIMEOUT_PROP_NAME);
        long timeout = DEFAULT_CONSUMER_READY_TIMEOUT;
        if (timeoutStr!=null) {
            timeout = Long.parseLong(timeoutStr);
        }
        if (timeout < MIN_CONSUMER_READY_TIMEOUT) {
            logger.warn("Consumer [{}]: Timeout read from properties {} is too short: using {} instead",
                    consumerID,
                    timeout,
                    MIN_CONSUMER_READY_TIMEOUT);
            timeout = MIN_CONSUMER_READY_TIMEOUT;
        }
        // Wait until the consumer is ready
        long now = System.currentTimeMillis();
        while (!isReady() && System.currentTimeMillis()<now+timeout) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                logger.warn("Interrupted");
                break;
            }
        }
        if (isReady()) {
            logger.debug("The consumer [{}] is ready", consumerID);
        } else {
        logger.warn("The consumer [{}] if not yet ready at the end of the timeout.", consumerID);
        }
    }
}
