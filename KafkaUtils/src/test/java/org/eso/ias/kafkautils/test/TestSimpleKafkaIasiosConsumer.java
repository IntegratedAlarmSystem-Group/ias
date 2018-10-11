package org.eso.ias.kafkautils.test;

import org.eso.ias.kafkautils.KafkaIasiosConsumer;
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSimpleKafkaIasiosConsumer {
    /**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(TestSimpleKafkaIasiosConsumer.class);

    /**
     * The consumer of IASIOs received from the Kafka topic
     */
    private SimpleKafkaIasiosConsumer consumer;
}
