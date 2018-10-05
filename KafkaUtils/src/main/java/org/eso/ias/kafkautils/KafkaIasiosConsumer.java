package org.eso.ias.kafkautils;

import org.eso.ias.kafkautils.KafkaStringsConsumer.StartPosition;
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
/**
 * KafkaIasiosConsumer gets the strings from the passed IASIO kafka topic
 * from the SimpleStringConsumer and forwards IASIOs to the listener.
 * <P>
 * Filtering is based on the ID of the IASIOs _and_ IASValue type: 
 * - if the ID of the received String is contained in {@link #acceptedIds}
 *   then the IASIO is forwarded to the listener otherwise is rejected.
 * - if the type of the {@link IASValue} is contained in {@link #acceptedTypes}
 *   then the IASIO is forwarded to the listener otherwise is rejected.
 * 
 * <BR>If the caller does not set any filter, then all the received IASIOs 
 * will be forwarded to the listener.
 * IDs and type are evaluated in AND. 
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
	private volatile Set<String> acceptedIds = Collections.unmodifiableSet(new HashSet<>());
	
	/**
	 * The set of accepted IDs of IASValue types for the filtering
	 */
	private volatile  Set<IASTypes> acceptedTypes = Collections.unmodifiableSet(new HashSet<>());
	
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
	 * @param idsOfIDsToAccept   The IDs of the IASIOs to forward to the listener
	 * @param idsOfTypesToAccept The types of the IASIOs to forward to the listener
	 */
	public KafkaIasiosConsumer(
			String servers, 
			String topicName, 
			String consumerID, 
			Set<String> idsOfIDsToAccept,
			Set<IASTypes> idsOfTypesToAccept) {
		this(servers, topicName, consumerID);
		setFilter(idsOfIDsToAccept, idsOfTypesToAccept);
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
			logger.error("Error building the IASValue from string [{}]: value lost",event,e);
			return;
		}
		if (accept(iasio)) {
			try {
				iasioListener.iasioReceived(iasio.updateReadFromBsdbTime(System.currentTimeMillis()));
			} catch (Exception e) {
				logger.error("Error notifying the IASValue [{}] to the listener: value lost",iasio.toString(),e);
			}
		}
	}

    /**
     * Accepts or rejects a IASValue against the filters, if set
     *
     * @param iasio The IASValue to accept or discard
     * @return true if teh value is accpted; false otherwise
     */
	private boolean accept(IASValue<?> iasio) {
		assert(iasio!=null);

		// Locally copy the sets that are immutable and volatile
        // In case the setFilter is called in the mean time...
		Set<String> acceptedIdsNow = acceptedIds;
		Set<IASTypes> acceptedTypesNow = acceptedTypes;

		boolean acceptedById = acceptedIdsNow.contains(iasio.id) || acceptedIdsNow.isEmpty();
		if (!acceptedById) {
		    return false;
        } else {
		    return acceptedTypesNow.isEmpty() || acceptedTypesNow.contains(iasio.valueType);
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
	 * Entirely remove the filtering 
	 */
	public void clearFilter() {
	    acceptedIds = Collections.unmodifiableSet(new HashSet<>());
	    acceptedTypes = Collections.unmodifiableSet(new HashSet<>());
	}
	
	/**
	 * Add the passed IDs to the filter
	 * 
	 * @param idsToAdd The filters to add
	 */
	public void addIdsToFilter(Set<String> idsToAdd) {
	    Objects.requireNonNull(idsToAdd);
	    Set<String> temp = new HashSet<>(acceptedIds);
	    temp.addAll(idsToAdd);
	    acceptedIds=Collections.unmodifiableSet(temp);

	}
	
	/**
	 * Add the passed alarm types to the filter
	 * 
	 * @param typesToAdd The types to add
	 */
	public void addTypesToFilter(Set<IASTypes> typesToAdd) {
	    Objects.requireNonNull(typesToAdd);

	    Set<IASTypes> temp = new HashSet<>(acceptedTypes);
	    temp.addAll(typesToAdd);

		acceptedTypes=Collections.unmodifiableSet(temp);
	}
	
	/** 
	 * Set the filtering to the passed set of IDs, discarding the 
	 * existing filters, if any.
	 * 
	 * @param idsToAccept   The new set of IDs for filtering; 
	 *                      if <code>null</code> or empty the filtering by IDs is removed
	 * @param typesToAccept The new set of types for filtering
	 *                      if <code>null</code> or empty the filtering by types is removed
	 */
	public void setFilter(Set<String> idsToAccept, Set<IASTypes> typesToAccept) {
	    if (idsToAccept==null) {
	       acceptedIds = Collections.unmodifiableSet(new HashSet<>());
        } else {
	       acceptedIds = Collections.unmodifiableSet(idsToAccept);
        }

        if (typesToAccept==null) {
           acceptedTypes = Collections.unmodifiableSet(new HashSet<>());
        } else {
           acceptedTypes = Collections.unmodifiableSet(typesToAccept);
        }
	}

    /**
     * Getter
     *
     * @return the unmodifiable set of accepted IDs
     */
	public Set<String> getAcceptedIds() {
	    return acceptedIds;
    }

    /**
     * Getter
     *
     * @return the unmodifiable set of accepted IAS types
     */
    public Set<IASTypes> getAcceptedTypes() {
        return acceptedTypes;
    }

}
