package org.eso.ias.kafkautils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.eso.ias.prototype.input.java.IASValue;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IasValueStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
/**
 * KafkaIasiosConsumer gets the strings from the passed IASIO kafka topic
 * from the SimpleStringConsumer and forwards IASIOs to the listener.
 * <P>
 * Filtering is based on the ID of the IASIOs: if the ID of the received String
 * is contained in {@link #acceptedIds} then the IASIO is forwarded to 
 * the listener otherwise is rejected.
 * <BR>If th caller does not set any filter, then all the received IASIOs 
 * will be forwarded to the listener. 
 * 
 * @author acaproni
 */
public class KafkaIasiosConsumer 
implements KafkaConsumerListener {

	/**
	 * The listener to be notified of Iasios read
	 * from the kafka topic.
	 *
	 * @author acaproni
	 *
	 */
	public interface IasioListener {

		/**
		 * Process an IASIO received from the kafka topic.
		 *
		 * @param event The IASIO received in the topic
		 */
		public void iasioReceived(IASValue<?> event);
	}
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(KafkaIasiosConsumer.class);
	
	/**
	 * The serializer/deserializer to convert the string
	 * received by the BSDB in a IASValue
	*/
	private final IasValueStringSerializer serializer = new IasValueJsonSerializer();
	
	/**
	 * The set of accepted IDs of IASIOs for the filtering
	 */
	private final Set<String> acceptedIds = Collections.synchronizedSet(new HashSet<>());
	
	/**
	 * The listener to be notified when a IASIOs is read from the kafka topic
	 * and accepted by the filtering.
	 */
	private IasioListener iasioListener;
	
	/**
	 * The string consumer to get strings from teh kafka topic
	 */
	private final SimpleStringConsumer stringConsumer;

	/**
	 * Build a FilteredStringConsumer with no filters (i.e. all the
	 * strings read from the kafka topic are forwarded to the listener)
	 * 
	 * @param servers The kafka servers to connect to
	 * @param topicName The name of the topic to get events from
	 * @param consumerID the ID of the consumer
	 */
	public KafkaIasiosConsumer(String servers, String topicName, String consumerID) {
		stringConsumer = new SimpleStringConsumer(servers, topicName, consumerID);
	}
	
	/**
	 * Build a FilteredStringConsumer with the passed initial filters.
	 * 
	 * @param servers The kafka servers to connect to
	 * @param topicName The name of the topic to get events from
	 * @param consumerID the ID of the consumer
	 * @param listener The listener of events published in the topic
	 * @param idsOfIasiosToAccept The IDs of the IASIOs to forward to the listener
	 */
	public KafkaIasiosConsumer(String servers, String topicName, String consumerID, Set<String> idsOfIasiosToAccept) {
		this(servers, topicName, consumerID);
		Objects.requireNonNull(idsOfIasiosToAccept);
		acceptedIds.addAll(idsOfIasiosToAccept);
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
	public void startGettingEvents(StartPosition startReadingFrom, IasioListener listener)
	throws KafkaUtilsException {
		Objects.requireNonNull(listener);
		this.iasioListener=listener;
		stringConsumer.startGettingEvents(startReadingFrom, this);
	}

	/** 
	 * Receive string published in the kafka topic and
	 * forward IASIOs to the listener
	 * 
	 * @param event The string read from the Kafka topic
	 */
	public void stringEventReceived(String event) {
		assert(event!=null && !event.isEmpty());
		IASValue<?> iasio=null;
		try {
			iasio = serializer.valueOf(event);
		} catch (Exception e) {
			logger.error("Error building the IASValue from the string [{}]: value lost",event,e);
			return;
		}
		if (acceptedIds.contains(iasio.id) || acceptedIds.isEmpty()) {
			try {
				iasioListener.iasioReceived(iasio);
			} catch (Exception e) {
				logger.error("Error notifying the IASValue [{}] to the listener: value lost",iasio.toString(),e);
			}
		}
	}

	/**
	 * Initializes the consumer with the passed kafka properties.
	 * <P>
	 * The defaults are used if not found in the parameter
	 *
	 * @param userPros The user defined kafka properties
	 */
	public void setUp(Properties userPros) {
		stringConsumer.setUp(userPros);
	}

	/**
	 * Initializes the consumer with default kafka properties
	 */
	public void setUp() {
		stringConsumer.setUp();
	}

	/**
	 * Close and cleanup the consumer
	 */
	public void tearDown() {
		stringConsumer.tearDown();
	}

	/**
	 * @return the number of records processed
	 */
	public long getNumOfProcessedRecords() {
		return stringConsumer.getNumOfProcessedRecords();
	}

	/**
	 * @return the number of strings processed
	 */
	public long getNumOfProcessedStrings() {
		return stringConsumer.getNumOfProcessedStrings();
	}
	
	/**
	 * Remove the filtering
	 */
	public void clearFilter() {
		acceptedIds.clear();
	}
	
	/**
	 * Add the passed IDs to the filter
	 * 
	 * @param idsToAdd The filters to add
	 */
	public void addIdsToFilter(Set<String> idsToAdd) {
		if (idsToAdd!=null) {
			acceptedIds.addAll(idsToAdd);
		}
	}
	
	/** 
	 * Set the filtering to the passed set of IDs, discarding the 
	 * existing filters, if any.
	 * 
	 * @param idsToAccept The new filters to set
	 */
	public void setFilter(Set<String> idsToAccept) {
		synchronized(acceptedIds) {
			acceptedIds.clear();
			if (idsToAccept!=null) {
				acceptedIds.addAll(idsToAccept);
			}
		}
	}

}
