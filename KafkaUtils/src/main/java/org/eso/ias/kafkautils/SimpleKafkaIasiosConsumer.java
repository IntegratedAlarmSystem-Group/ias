package org.eso.ias.kafkautils;

import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * KafkaIasiosConsumer gets the strings from the passed IASIO kafka topic
 * from the {@link KafkaStringsConsumer} and forwards the IASIOs to the listener.
 *
 * The SimpleKafkaIasiosConsumer checks the timestaps of the received IASIOs against a threshold.
 * If the timestamp is too old it means that the reciver is slow processing events: old IASIOs are discarded,
 * a log is emitted and the consumers seeks to the end of the topic.
 *
 * To decide what old means, the user must set the {@link #SeekIfOlderThanProName} to the number
 * of desired milliseconds otherwise {@link #SeekIfOlderThanDefault} is used
 */
public class SimpleKafkaIasiosConsumer implements KafkaStringsConsumer.StringsConsumer {
    /**
     * The listener to be notified of Iasios read
     * from the kafka topic.
     *
     * @author acaproni
     *
     */
    public interface IasioListener {

        /**
         * Process the IASIOs read from the kafka topic.
         *
         * @param events The IASIOs received in the topic
         */
        void iasiosReceived(Collection<IASValue<?>> events);
    }

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SimpleKafkaIasiosConsumer.class);

    /**
     * The serializer/deserializer to convert the string
     * received by the BSDB in a IASValue
     */
    private final IasValueStringSerializer serializer = new IasValueJsonSerializer();

    /**
     * The listener to be notified when a IASIOs is read from the kafka topic
     * and accepted by the filtering.
     */
    private KafkaIasiosConsumer.IasioListener iasioListener;

    /**
     * The string consumer to get strings from teh kafka topic
     */
    private final KafkaStringsConsumer stringsConsumer;

    /**
     * The property to set the number of milliseconds between the actual time and the time of
     * the records and seek to the end of the topic
     */
    private static final String SeekIfOlderThanProName="org.eso.ias.kafkautils.iasioconsumer.seekifolderthan";

    /**
     * The default number of milliseconds between the actual time and the time when records have been
     * sent to the kafka topic to seek to the end of the topic
     * It is disabled by default.
     *
     * @see #SeekIfOlderThan
     */
    private static final long SeekIfOlderThanDefault = Long.MAX_VALUE;

    /**
     * If the difference (millisecs) between the timestamp of the records read from kafka topic
     * and the actual time is greater than seekIfOlderThan, then the consumer seek
     * to the end of topic
     */
    private static final long SeekIfOlderThan=Long.getLong(
            SimpleKafkaIasiosConsumer.SeekIfOlderThanProName,
            SimpleKafkaIasiosConsumer.SeekIfOlderThanDefault);

    /**
     * Build a FilteredStringConsumer with no filters (i.e. all the
     * strings read from the kafka topic are forwarded to the listener)
     *
     * @param servers The kafka servers to connect to
     * @param topicName The name of the topic to get events from
     * @param consumerID the ID of the consumer
     */
    public SimpleKafkaIasiosConsumer(String servers, String topicName, String consumerID) {
        stringsConsumer = new SimpleStringConsumer(servers, topicName, consumerID);
    }

    /**
     * Start processing received by the SimpleStringConsumer from the kafka channel.
     * <P>
     * This method starts the thread that polls the kafka topic
     * and returns after the consumer has been assigned to at least
     * one partition.
     *
     * @param startReadingFrom Starting position in the kafka partition
     * @param listener The listener of events published in the topic
     * @throws KafkaUtilsException in case of timeout subscribing to the kafkatopic
     */
    public void startGettingEvents(KafkaStringsConsumer.StreamPosition startReadingFrom, KafkaIasiosConsumer.IasioListener listener)
            throws KafkaUtilsException {
        Objects.requireNonNull(listener);
        this.iasioListener=listener;
        stringsConsumer.startGettingEvents(this,startReadingFrom);
    }

    /**
     * Receive the string consumed from the kafka topic and
     * forward the IASIOs to the listener
     *
     * @param strings The strings read from the Kafka topic
     */
    @Override
    public void stringsReceived(Collection<String> strings) {
        assert(strings!=null && !strings.isEmpty());

        // Store the max difference between the actual timestamp
        // and the time when the passed IASValuehave been sent to the
        // kafka the topic
        long maxTimeDifference=0;
        Collection<IASValue<?>> ret = new ArrayList<>();

        Iterator<String> iterator = strings.iterator();
        while (iterator.hasNext()) {
            String str = iterator.next();
            IASValue<?> iasio;
            try {
                iasio = serializer.valueOf(str);
            } catch (Exception e) {
                logger.error("Error building the IASValue from string [{}]: value lost",str,e);
                continue;
            }
            if (iasio.sentToBsdbTStamp.isPresent()) {
                maxTimeDifference = Long.max(maxTimeDifference,System.currentTimeMillis()-iasio.sentToBsdbTStamp.get());
            };
            if (accept(iasio)) {
                ret.add(iasio.updateReadFromBsdbTime(System.currentTimeMillis()));
            }
        }

        // Check if the porcessed records are too old and publishes only if they are enough recent
        // If they are too old than the consumer is too slow processing events and seek to the end
        if (maxTimeDifference<=SimpleKafkaIasiosConsumer.SeekIfOlderThan) {
            try {
                logger.debug("Notifying {} IASIOs to the listener listener", ret.size());
                if (!ret.isEmpty()) {
                    iasioListener.iasiosReceived(ret);
                }
            } catch (Exception e) {
                logger.error("Error notifying IASValues to the listener: {} values potentially lost", ret.size(), e);
            }
        } else {
            logger.warn("Consumer too slow processing events: seeking to the end of the topic ({} ISOIOs discarded)",ret.size());
            boolean seekOk=stringsConsumer.seekTo(KafkaStringsConsumer.StreamPosition.END);
            if (!seekOk) {
                logger.error("Cannot seek to end of stream");
            }
        }
    }

    /**
     * Accept all the IASValues.
     *
     * Objects that inherit from {@link SimpleKafkaIasiosConsumer} can implement their
     * filtering by overriding this method.
     *
     * @param iasio The IASValue to accept or discard
     * @return true if teh value is accpted; false otherwise
     */
    protected boolean accept(IASValue<?> iasio) {
        assert(iasio!=null);

       return true;
    }

    /**
     * Initializes the consumer with the passed kafka properties.
     * <P>
     * The defaults are used if not found in the parameter
     *
     * @param userPros The user defined kafka properties
     */
    public void setUp(Properties userPros) {
        stringsConsumer.setUp(userPros);
    }

    /**
     * Initializes the consumer with default kafka properties
     */
    public void setUp() {
        stringsConsumer.setUp();
    }

    /**
     * Close and cleanup the consumer
     */
    public void tearDown() {
        stringsConsumer.tearDown();
    }

    /**
     * @return the number of records processed
     */
    public long getNumOfProcessedRecords() {
        return stringsConsumer.getNumOfProcessedRecords();
    }

}
