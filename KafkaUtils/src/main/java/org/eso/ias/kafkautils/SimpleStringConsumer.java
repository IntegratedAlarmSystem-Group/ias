package org.eso.ias.kafkautils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * /**
 * Generic Kafka consumer to get strings from a kafka topic.
 * <P>
 * The strings are passed one at a time to the listener for further processing.
 *
 *
 * @author acaproni
 *
 */
public class SimpleStringConsumer extends  KafkaStringsConsumer implements KafkaStringsConsumer.StringsConsumer {

	/**
	 * The listener to be notified of strings read
	 * from the kafka topic.
	 *
	 * @author acaproni
	 *
	 */
	public interface KafkaConsumerListener {

		/**
		 * Process an event (a String) received from the kafka topic
		 *
		 * @param event The string received in the topic
		 */
		public void stringEventReceived(String event);
	}



	/**
	 * The listener of events published in the topic
	 */
	private KafkaConsumerListener stringListener;

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SimpleStringConsumer.class);

	/**
	 * The number of strings received (a record can contain more strings)
	 */
	private final AtomicLong processedStrings = new AtomicLong(0);

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
	 * Constructor
	 *
	 * @param servers The kafka servers to connect to
	 * @param topicName The name of the topic to get events from
	 * @param consumerID the ID of the consumer
	 */
	public SimpleStringConsumer(String servers, String topicName, String consumerID) {
		super(servers,topicName,consumerID);
	}

	/**
	 * Start polling events from the kafka channel.
	 * <P>
	 * This method starts the thread that polls the kafka topic
	 * and returns after the consumer has been assigned to at least
	 * one partition.
	 *
	 * @param startReadingFrom Starting position in the kafka partition
	 * @param listener The listener of events published in the topic
	 * @throws KafkaUtilsException in case of timeout subscribing to the kafkatopic
	 */
	public synchronized void startGettingEvents(StartPosition startReadingFrom, KafkaConsumerListener listener)
	throws KafkaUtilsException {
	    Objects.requireNonNull(listener);
	    super.startGettingEvents(this,startReadingFrom);
        stringListener=listener;
	}

    /**
     * Sends the strings received from the kafka topic
     * to the listener
     */
    @Override
    public void stringsReceived(Collection<String> strings) {
        strings.forEach( str -> {
            try {
                if (!str.isEmpty()) {
                    stringListener.stringEventReceived(str);
                    processedStrings.incrementAndGet();
                }
            } catch (Exception e) {
                SimpleStringConsumer.logger.error("The listener threw exception processing the string [{}]",str,e);
            }
        });
    }
	
	/**
	 * @return the number of strings processed
	 */
	public long getNumOfProcessedStrings() {
		return processedStrings.get();
	}

}
