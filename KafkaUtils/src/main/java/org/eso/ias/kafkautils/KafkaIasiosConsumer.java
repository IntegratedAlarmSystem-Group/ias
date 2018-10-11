package org.eso.ias.kafkautils;

import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
/**
 * Extends {@link SimpleKafkaIasiosConsumer} providing filtering by IDs and types.
 * <P>
 * Filtering is based on the ID of the IASIOs _and_ the IASValue type:
 * - if the ID of the received String is contained in {@link #acceptedIds}
 *   then the IASIO is forwarded to the listener otherwise it is rejected.
 * - if the type of the {@link IASValue} is contained in {@link #acceptedTypes}
 *   then the IASIO is forwarded to the listener otherwise is rejected.
 * 
 * <P>If the caller does not set any filter, then all the received IASIOs
 * will be forwarded to the listener.
 *
 * IDs and type are evaluated in AND. 
 * 
 * @author acaproni
 * @deprecated Use {@link FilteredKafkaIasiosConsumer} instead
 */
public class KafkaIasiosConsumer extends SimpleKafkaIasiosConsumer {

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
		super(servers, topicName, consumerID);
		setFilter(idsOfIDsToAccept, idsOfTypesToAccept);
	}
	

    /**
     * Accepts or rejects a IASValue against the filters, if set
     *
     * @param iasio The IASValue to accept or discard
     * @return true if teh value is accpted; false otherwise
     */
    @Override
	protected boolean accept(IASValue<?> iasio) {
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
