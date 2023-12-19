package org.eso.ias.webserversender;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.publisher.HbLogProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.types.*;
import org.eso.ias.webserversender.WebServerSender.WebServerSenderListener;
import org.eso.ias.webserversender.WebSocketServerHandler.WebSocketServerListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that, when messages are published to the Kafka queue,
 * the WebServerSender can read them and send them to the WebServer through
 * a Websocket connection
 *
 * @author Inria Chile
 *
 */
public class WebServerSenderTest {

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WebServerSenderTest.class);

	/**
	 * Kafka Topic to used in the test
	 */
	private final String kafkaTopic = "webserver-sender-test";

	/**
	 * Kafka Server to connect to
	 */
	private final String kafkaServer = KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS;

	/**
	 * Port to connect to the mock Websocket Server
	 */
	private final int port = 10000;

	/**
	 * URI to connect to the mock Websocket Server
	 */
	private final String webserverUri = "ws://localhost:" + this.port + "/";

	/**
	 * Number of messages to be used in the test
	 */
	private final int messagesNumber=25000;

	/**
	* Collection of Iasios to be used in the test
	*/
	private Collection<IASValue<?>> iasValuesToSend;

	/**
	 * The WebServerSender we are testing
	 */
	private WebServerSender webServerSender;

	/**
	 * A mock Server to receive messages sent by the WebServerSender
	 */
	private Server server;

	/**
	 * A mock producer to publish messages to the KafkaQueue
	 */
	private KafkaIasiosProducer producer;

	/**
	 * Countdown of the messages sent by the WebServerSender
	 */
	private CountDownLatch messagesSentByWSS;

	/**
	 * Countdown of the messages received by the Mock Server
	 */
	private CountDownLatch numOfIasValuesToReceive;

	/**
	 * Countdown to initialize the test when the server is ready
	 */
	private CountDownLatch serverReady;

	/**
	 * The list of strings sent by the sender.
	 * <P>
	 * It contains only the strings that this test produced
	 */
	private final List<String> listsOfIasValuesSentByWSS = Collections.synchronizedList(new ArrayList<>());

	/**
	 * The list of strings received by the server.
	 * <P>
	 * It contains only the strings that this test produced
	 */
	private final List<IasValueJsonPojo> receivedIasValues = Collections.synchronizedList(new ArrayList<>());

	/**
	 * The serializer/deserializer to convert the string
	 * received by the BSDB in a IASValue
	 */
	private final IasValueStringSerializer serializer = new IasValueJsonSerializer();

	/**
	 * The jackson 2 mapper
	 */
	private final ObjectMapper jsonMapper = new ObjectMapper();


	/**
	 * Auxiliary method to start the mock Server
	 */
	private void runServer() throws Exception {
		this.server = new Server(this.port);
        WebSocketHandler wsHandler = new WebSocketServerHandler();
        WebSocketServerListener listener = new WebSocketServerListener(){
        	public void stringEventSent(String event) {

				IasValueJsonPojo[] iasValues=null;
        		try{
        			iasValues = jsonMapper.readValue(event, IasValueJsonPojo[].class);
				} catch (Exception e) {
        			logger.error("Exception unmarshalling {}",event);
        			e.printStackTrace();
        			return;
				}


				for (int t=0; t<iasValues.length; t++) {
					receivedIasValues.add(iasValues[t]);
					numOfIasValuesToReceive.countDown();
				}

        	};
        };
        WebSocketServerHandler.setListener(listener);
        this.server.setHandler(wsHandler);
		this.server.start();
		logger.info("Server initialized...");
	}

	/**
	 * Auxiliary method to stop and then restart the mock Server
	 */
	private void restartServer() throws Exception {
		this.server.stop();
		this.serverReady = new CountDownLatch(1);
		this.runServer();
	}

	/**
	 * Auxiliary method to start the WebServerSender
	 */
	private void runSender() throws Exception {
        WebServerSenderListener listener = new WebServerSenderListener(){
        	public void stringEventSent(String event) {
        		if(event.equals("Hello")) {
        			serverReady.countDown();
        		}
        		else {
	        		listsOfIasValuesSentByWSS.add(event);
	        		messagesSentByWSS.countDown();
	        	}
        	};
        };
		Properties props = new Properties();
		props.put("org.eso.ias.senders.kafka.inputstream", this.kafkaTopic);
		props.put("org.eso.ias.senders.kafka.servers", this.kafkaServer);
		props.put("org.eso.ias.senders.webserver.uri", this.webserverUri);


		HbMsgSerializer hbSer = new HbJsonSerializer();
		HbProducer hProd = new HbLogProducer(hbSer);

		Set<String> idsOfIDsToAccept = new HashSet<>();
		Set<IASTypes> idsOfTypesToAccept = new HashSet<>();

		this.webServerSender = new WebServerSender(
				"WebServerSender",
				KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
				props,
				listener,
				1,
				idsOfIDsToAccept,
				idsOfTypesToAccept);
		this.webServerSender.setUp();
		logger.info("WebServerSender initialized...");
	}

	/**
	 * Build the full running ID from the passed id
	 *
	 * @param id The Id of the IASIO
	 * @return he full running ID
	 */
	private String buildFullRunningID(String id) {
		return Identifier.coupleGroupPrefix()+id+Identifier.coupleSeparator()+"IASIO"+Identifier.coupleGroupSuffix();
	}

	/**
	 * Build and return the IASValues of the given type to publish from the passed IDs
	 *
	 * @param ids The Ids of the IASValues to build
	 * @param value the value of the IASValues
	 * @param type the type of the IASValues
	 * @return The IASValues to publish
	 */
	public Collection<IASValue<?>> buildValues(List<String> ids, Object value, IASTypes type) {
		Objects.requireNonNull(ids);
		return ids.stream().map(id ->
			IASValue.build(
					value,
					OperationalMode.OPERATIONAL,
					IasValidity.RELIABLE,
					buildFullRunningID(id),
					type)
		).collect(Collectors.toList());
	}

	/**
	 * Test set up
	 */
	@BeforeEach
	public void setUp() throws Exception {
		logger.info("Starting Test Initialization");

		List<String> idsOfLongs = new LinkedList<>();
		for (int i=0; i<this.messagesNumber; i++) idsOfLongs.add("ID-TypeLong-"+i);
		this.iasValuesToSend = buildValues(idsOfLongs, 10L, IASTypes.LONG);

		this.numOfIasValuesToReceive = new CountDownLatch(messagesNumber);
		this.messagesSentByWSS = new CountDownLatch(messagesNumber);
		this.serverReady = new CountDownLatch(1);

		SimpleStringProducer stringProd = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,"ProducerTest");
		this.producer = new KafkaIasiosProducer(stringProd, kafkaTopic, this.serializer);
		this.producer.setUp();

		this.runServer();
		this.runSender();

		Boolean status = this.serverReady.await(10, TimeUnit.SECONDS);
		assertTrue(status,"Server and sender never connected");

		logger.info("Test Initialization Completed.");
	}

	/**
	 * Test that when messages are published to the Kafka queue,
	 * the WebServerSender can read them and send them to the WebServer through
	 *  a Websocket connection
	 */
	@Test
	public void testWebServerSender() throws Exception {
		// Arrange
		logger.info("Starting Test Execution");
		int iasValuesDeliveredToKafka = 0;

		// Act:
		for (IASValue<?> value: this.iasValuesToSend) {
			try {
				this.producer.push(value);
				this.producer.flush();
				iasValuesDeliveredToKafka += 1;
			}
			catch (Exception e) {
				logger.error("Message was not published to the Kafka Queue by Mock Producer");
			}
		}

		logger.info("Sent {} IASValues to kafka", iasValuesDeliveredToKafka);

		boolean timeout =this.numOfIasValuesToReceive.await(10, TimeUnit.SECONDS);
		if (!timeout) {
			for (IASValue<?> value: this.iasValuesToSend) {
				if (!this.receivedIasValues.contains(serializer.iasValueToString(value))) {
					logger.error("[{}] never received by the server",serializer.iasValueToString(value));
				}
			}
		}
		assertTrue(timeout,"Timeout receiving masseges");

		// Assert:
		logger.info("Test Execution Completed");
		logger.info("Summary:");
		logger.info("- [{}] IasValues were published in the Kafka topic",iasValuesDeliveredToKafka);
		logger.info("- [{}] messages were sent by the WebServerSender",this.listsOfIasValuesSentByWSS.size());
		logger.info("- [{}] IasValues were received by the Mock Server",this.receivedIasValues.size());
		assertEquals(messagesNumber, iasValuesDeliveredToKafka,"Some messages were not published in the Kafka Queue by Mock Producer");
		assertEquals(iasValuesDeliveredToKafka,receivedIasValues.size(), "Some IasValues were not sent by the WebServerSender");
		assertEquals(messagesNumber, this.receivedIasValues.size(),"Some messages were not received by the Mock Server");
	}

	/**
	 * Test that the WebserverSender reconnects and resume working
	 * if the Server fails and then recovers
	 */
	@Test
	public void testReconnection() throws Exception {
		// Arrange
		this.numOfIasValuesToReceive = new CountDownLatch(messagesNumber/2);
		this.messagesSentByWSS = new CountDownLatch(messagesNumber/2);
		logger.info("Starting Test Execution");
		int iasValuesDeliveredToKafka = 0;

		// Act:
		// Send half of the messages:
		Iterator<IASValue<?>> iterator = iasValuesToSend.iterator();
		while(iterator.hasNext()) {
			try {
				IASValue<?> value = iterator.next();
				this.producer.push(value);
				this.producer.flush();
				iasValuesDeliveredToKafka += 1;
			}
			catch (Exception e) {
				logger.error("Message was not published to the Kafka Queue by Mock Producer");
			}
			if (iasValuesDeliveredToKafka >= messagesNumber / 2) {
				break;
			}
		}

		// Stop and restart the Server:
		this.messagesSentByWSS.await(5, TimeUnit.SECONDS);
		this.restartServer();
		this.numOfIasValuesToReceive = new CountDownLatch(messagesNumber/2);
		this.messagesSentByWSS = new CountDownLatch(messagesNumber/2);

		Boolean status = this.serverReady.await(10, TimeUnit.SECONDS);
		assertTrue(status,"Sender did not reconnect to Server");

		// Send the other half of the messages:
		while(iterator.hasNext()) {
			try {
				IASValue<?> value = iterator.next();
				this.producer.push(value);
				this.producer.flush();
				iasValuesDeliveredToKafka += 1;
			}
			catch (Exception e) {
				logger.error("Message was not published to the Kafka Queue by Mock Producer");
			}
		}

		logger.info("{} messages delivered", iasValuesDeliveredToKafka);

		if (!this.numOfIasValuesToReceive.await(5, TimeUnit.SECONDS)) {
			for (IASValue<?> value: this.iasValuesToSend) {
				if (!this.receivedIasValues.contains(serializer.iasValueToString(value))) {
					logger.error("[{}] never received by the server",serializer.iasValueToString(value));
				}
			}
		}

		// Assert:
		logger.info("Test Reconnection Execution Completed");
		logger.info("Summary:");
		logger.info("- [{}] messages were published in the Kafka queue",iasValuesDeliveredToKafka);
		logger.info("- [{}] IasValues were sent by the WebServerSender",this.listsOfIasValuesSentByWSS.size());
		logger.info("- [{}] IasValues were received by the Mock Server",this.receivedIasValues.size());
		assertEquals(messagesNumber, iasValuesDeliveredToKafka,"Some messages were not published in the Kafka Queue by Mock Producer");
		assertEquals(iasValuesDeliveredToKafka, this.receivedIasValues.size(),"Some messages were not received by the Mock Server");
	}

	/**
	 * Stop the WebserverSender, Server and Producer after the test is finished
	 */
	@AfterEach
	public void tearDown() throws Exception {
		logger.info("Starting Test Finalization");
		this.producer.tearDown();
		this.webServerSender.close();
		this.server.stop();
		logger.info("Test Finalization Completed");
	}
}
