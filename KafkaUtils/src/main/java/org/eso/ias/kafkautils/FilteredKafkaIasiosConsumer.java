package org.eso.ias.kafkautils;

import org.eso.ias.types.IASValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * FilteredKafkaIasiosConsumer allows to set a custom filter on top of the Kafka consumer.
 *
 * The filter implements the FilterIaValue interface.
 *
 */
public class FilteredKafkaIasiosConsumer extends SimpleKafkaIasiosConsumer {

    /**
     * The filter to apply to IASValues
     */
    public interface FilterIasValue {

        /**
         * Accepts or discards the value according to the filter: te value is accepted
         * if no value is set
         *
         * @param value The value to check against the filter
         * @return true if the value must be accpeted, false to discard
         */
        public boolean accept(IASValue<?> value);
    }

    /**
     * The logger
     */
    private static final Logger logger = LoggerFactory.getLogger(FilteredKafkaIasiosConsumer.class);

    /**
     * The filter to accept or discard values
     */
    private final FilterIasValue filter;

    /**
     * Constructor
     *
     * @param servers The kafka servers to connect to
     * @param topicName  The name of the topic to get events from
     * @param consumerID  the ID of the consumer
     * @param filter The filter to apply to accept or discard IASValues
     */
    public FilteredKafkaIasiosConsumer(
            String servers,
            String topicName,
            String consumerID,
            FilterIasValue filter) {
        super(servers,topicName,consumerID);
        Objects.requireNonNull(filter);
        this.filter=filter;
    }

    /**
     * Accepts or rejects a IASValue against the filters, if set
     *
     * @param iasio The IASValue to accept or discard
     * @return true if the value is accpted; false otherwise
     */
    @Override
    protected boolean accept(IASValue<?> iasio) {
        try {
            return filter.accept(iasio);
        } catch (Exception e) {
            FilteredKafkaIasiosConsumer.logger.error("Exception got while filtering: value lost",e);
            return false;
        }
    }
}
