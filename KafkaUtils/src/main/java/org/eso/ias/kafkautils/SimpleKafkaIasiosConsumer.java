package org.eso.ias.kafkautils;

import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * KafkaIasiosConsumer gets the strings from the passed IASIO kafka topic
 * from the {@link KafkaStringsConsumer} and forwards the IASIOs to the listener.
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
    public void startGettingEvents(KafkaStringsConsumer.StartPosition startReadingFrom, KafkaIasiosConsumer.IasioListener listener)
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

        Collection<IASValue<?>> ret = new ArrayList<>();
        strings.forEach( str -> {
            IASValue<?> iasio;
            try {
                iasio = serializer.valueOf(str);
            } catch (Exception e) {
                logger.error("Error building the IASValue from string [{}]: value lost",str,e);
                return;
            }
            if (accept(iasio)) {
                ret.add(iasio.updateReadFromBsdbTime(System.currentTimeMillis()));
            }
        });

        try {
            logger.debug("Notifying {} IASIOs to the listener listener",ret.size());
            if (!ret.isEmpty()) {
                iasioListener.iasiosReceived(ret);
            }
        } catch (Exception e) {
            logger.error("Error notifying IASValues to the listener: {} values potentially lost",ret.size(),e);
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
