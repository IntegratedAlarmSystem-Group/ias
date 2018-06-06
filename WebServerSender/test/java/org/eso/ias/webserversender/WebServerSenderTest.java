package org.eso.ias.webserversender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.publisher.HbLogProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosProducer;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValidity;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueStringSerializer;
import org.eso.ias.types.Identifier;
import org.eso.ias.types.OperationalMode;
import org.eso.ias.webserversender.WebServerSender.WebServerSenderListener;
import org.eso.ias.webserversender.WebSocketServerHandler.WebSocketServerListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final String kafkaServer = "localhost:9092";

	/**
	 * Port to connect to the mock Websocket Server
	 */
	private final int port = 8081;

	/**
	 * URI to connect to the mock Websocket Server
	 */
	private final String webserverUri = "ws://localhost:" + this.port + "/";

	/**
	 * Number of messages to be used in the test
	 */
	private int messagesNumber;

	/**
	* Collection of Iasios to be used in the test
	*/
	private Collection<IASValue<?>> iasios;

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
	private CountDownLatch numOfMessagesToSend;

	/**
	 * Countdown of the messages received by the Mock Server
	 */
	private CountDownLatch numOfMessagesToReceive;

	/**
	 * Countdown to initialize the test when the server is ready
	 */
	private CountDownLatch serverReady;

	/**
	 * The list of strings sent by the sender.
	 * <P>
	 * It contains only the strings that this test produced
	 */
	private final List<String> sentMessages = Collections.synchronizedList(new ArrayList<>());

	/**
	 * The list of strings received by the server.
	 * <P>
	 * It contains only the strings that this test produced
	 */
	private final List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

	/**
	 * The serializer/deserializer to convert the string
	 * received by the BSDB in a IASValue
	 */
	private final IasValueStringSerializer serializer = new IasValueJsonSerializer();


	/**
	 * Auxiliary method to start the mock Server
	 */
	private void runServer() throws Exception {
		this.server = new Server(this.port);
        WebSocketHandler wsHandler = new WebSocketServerHandler();
        WebSocketServerListener listener = new WebSocketServerListener(){
        	public void stringEventSent(String event) {
        		receivedMessages.add(event);
        		numOfMessagesToReceive.countDown();
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
	        		sentMessages.add(event);
	        		numOfMessagesToSend.countDown();
	        	}
        	};
        };
		Properties props = new Properties();
		props.put("org.eso.ias.senders.kafka.inputstream", this.kafkaTopic);
		props.put("org.eso.ias.senders.kafka.servers", this.kafkaServer);
		props.put("org.eso.ias.senders.webserver.uri", this.webserverUri);
		
		
		HbMsgSerializer hbSer = new HbJsonSerializer();
		HbProducer hProd = new HbLogProducer(hbSer);
		
		
		this.webServerSender = new WebServerSender(
				"WebServerSender",
				KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS,
				props, 
				listener,
				1,
				hProd);
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

		this.messagesNumber = 6;
		List<String> idsOfLongs = new LinkedList<>();
		for (int i=0; i<this.messagesNumber; i++) idsOfLongs.add("ID-TypeLong-"+i);
		this.iasios = buildValues(idsOfLongs, 10L, IASTypes.LONG);

		this.numOfMessagesToReceive = new CountDownLatch(messagesNumber);
		this.numOfMessagesToSend = new CountDownLatch(messagesNumber);
		this.serverReady = new CountDownLatch(1);

		this.producer = new KafkaIasiosProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, kafkaTopic, "ProducerTest", this.serializer);
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
		int messagesDelivered = 0;

		// Act:
		for (IASValue<?> value: this.iasios) {
			try {
				this.producer.push(value);
				this.producer.flush();
				messagesDelivered += 1;
			}
			catch (Exception e) {
				logger.error("Message was not published to the Kafka Queue by Mock Producer");
			}
		}

		logger.info("{} messages delivered", messagesDelivered);

		if (!this.numOfMessagesToSend.await(10, TimeUnit.SECONDS)) {
			for (IASValue<?> value: this.iasios) {
				if (!this.sentMessages.contains(serializer.iasValueToString(value))) {
					logger.error("[{}] never sent by the sender",serializer.iasValueToString(value));
				}
			}
		}

		if (!this.numOfMessagesToReceive.await(10, TimeUnit.SECONDS)) {
			for (IASValue<?> value: this.iasios) {
				if (!this.receivedMessages.contains(serializer.iasValueToString(value))) {
					logger.error("[{}] never received by the server",serializer.iasValueToString(value));
				}
			}
		}

		// Assert:
		logger.info("Test Execution Completed");
		logger.info("Summary:");
		logger.info("- [{}] messages were published in the Kafka queue",messagesDelivered);
		logger.info("- [{}] messages were sent by the WebServerSender",this.sentMessages.size());
		logger.info("- [{}] messages were received by the Mock Server",this.receivedMessages.size());
		assertEquals(messagesNumber, messagesDelivered,"Some messages were not published in the Kafka Queue by Mock Producer");
		assertEquals(messagesNumber, this.sentMessages.size(),"Some messages were not sent by the WebServerSender");
		assertEquals(messagesNumber, this.receivedMessages.size(),"Some messages were not received by the Mock Server");
	}

	/**
	 * Test that the WebserverSender reconnects and resume working
	 * if the Server fails and then recovers
	 */
	@Test
	public void testReconnection() throws Exception {
		// Arrange
		this.numOfMessagesToReceive = new CountDownLatch(messagesNumber/2);
		this.numOfMessagesToSend = new CountDownLatch(messagesNumber/2);
		logger.info("Starting Test Execution");
		int messagesDelivered = 0;

		// Act:
		// Send half of the messages:
		Iterator<IASValue<?>> iterator = iasios.iterator();
		while(iterator.hasNext()) {
			try {
				IASValue<?> value = iterator.next();
				this.producer.push(value);
				this.producer.flush();
				messagesDelivered += 1;
			}
			catch (Exception e) {
				logger.error("Message was not published to the Kafka Queue by Mock Producer");
			}
			if (messagesDelivered >= messagesNumber / 2) {
				break;
			}
		}

		// Stop and restart the Server:
		this.numOfMessagesToSend.await(5, TimeUnit.SECONDS);
		this.restartServer();
		this.numOfMessagesToReceive = new CountDownLatch(messagesNumber/2);
		this.numOfMessagesToSend = new CountDownLatch(messagesNumber/2);

		Boolean status = this.serverReady.await(10, TimeUnit.SECONDS);
		assertTrue(status,"Sender did not reconnect to Server");

		// Send the other half of the messages:
		while(iterator.hasNext()) {
			try {
				IASValue<?> value = iterator.next();
				this.producer.push(value);
				this.producer.flush();
				messagesDelivered += 1;
			}
			catch (Exception e) {
				logger.error("Message was not published to the Kafka Queue by Mock Producer");
			}
		}

		logger.info("{} messages delivered", messagesDelivered);

		if (!this.numOfMessagesToSend.await(5, TimeUnit.SECONDS)) {
			for (IASValue<?> value: this.iasios) {
				if (!this.sentMessages.contains(serializer.iasValueToString(value))) {
					logger.error("[{}] never sent by the sender",serializer.iasValueToString(value));
				}
			}
		}

		if (!this.numOfMessagesToReceive.await(5, TimeUnit.SECONDS)) {
			for (IASValue<?> value: this.iasios) {
				if (!this.receivedMessages.contains(serializer.iasValueToString(value))) {
					logger.error("[{}] never received by the server",serializer.iasValueToString(value));
				}
			}
		}

		// Assert:
		logger.info("Test Reconnection Execution Completed");
		logger.info("Summary:");
		logger.info("- [{}] messages were published in the Kafka queue",messagesDelivered);
		logger.info("- [{}] messages were sent by the WebServerSender",this.sentMessages.size());
		logger.info("- [{}] messages were received by the Mock Server",this.receivedMessages.size());
		assertEquals(messagesNumber, messagesDelivered,"Some messages were not published in the Kafka Queue by Mock Producer");
		assertEquals(messagesNumber, this.sentMessages.size(),"Some messages were not sent by the WebServerSender");
		assertEquals(messagesNumber, this.receivedMessages.size(),"Some messages were not received by the Mock Server");
	}

	/**
	 * Stop the WebserverSender, Server and Producer after the test is finished
	 */
	@AfterEach
	public void tearDown() throws Exception {
		logger.info("Starting Test Finalization");
		this.producer.tearDown();
		this.webServerSender.shutdown();
		this.server.stop();
		logger.info("Test Finalization Completed");
	}
}
