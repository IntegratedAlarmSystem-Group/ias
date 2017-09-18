package org.eso.ias.kafkautils.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test the consumer and producer by submitting strings
 * and checking their reception.
 * 
 * @author osboxes
 *
 */
public class ConsumerProducerTest implements KafkaConsumerListener {
	
	/**
	 * The number of events to wait for
	 */
	private CountDownLatch numOfEventsToReceive = new CountDownLatch(5);
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(ConsumerProducerTest.class);
	
	/**
	 * The consumer to get events
	 */
	private SimpleStringConsumer consumer;
	
	/**
	 * The producer to pubish events
	 */
	private SimpleStringProducer producer;
	
	/**
	 * The topic to send to and receive strings from
	 */
	private final String topicName = "ConsumerProducerTest-topic";

	/**
	 * Initialize
	 */
	public void setUp() {
		logger.info("Initializing...");
		producer = new SimpleStringProducer(SimpleStringProducer.defaultBootstrapServers, topicName, "Consumer-ID");
		producer.setUp(System.getProperties());
		consumer = new SimpleStringConsumer(topicName, "localhost", 9092, this);
		consumer.setUp();
		logger.info("Initialized.");
	}
	
	/**
	 * Clean up
	 */
	public void tearDown() {
		logger.info("Closing...");
		consumer.tearDown();
		producer.tearDown();
		logger.info("Closed.");
	}
	
	public CountDownLatch getCountDownLatch() {
		return numOfEventsToReceive;
	}
	
	/**
	 * Publish the passed strings in the topic
	 * 
	 * @param strings
	 */
	public void send(String[] strings) {
		for (String str: strings) {
			producer.push(str, null, str);
		}
	}

	public static void main(String[] args) {
		String[] stringsToSend = {
				"Uno",
				"Due",
				"Tre",
				"Quattro",
				"Cinque"
		};
		
		logger.info("Process started");
		ConsumerProducerTest tester = new ConsumerProducerTest();
		tester.setUp();
		tester.send(stringsToSend);
		logger.info("{} strings sent",stringsToSend.length);
		CountDownLatch latch=tester.getCountDownLatch();
		boolean timedOut=true;
		try {
			timedOut=latch.await(1, TimeUnit.MINUTES);
		} catch (InterruptedException ie) {
			logger.info("Interrupted!");
		}
		tester.tearDown();
		logger.info("Done with All events received? {}",timedOut);
	}

	/* (non-Javadoc)
	 * @see org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener#stringEventReceived(java.lang.String)
	 */
	@Override
	public void stringEventReceived(String event) {
		logger.info("String received [{}]",event);
		numOfEventsToReceive.countDown();
	}

}
