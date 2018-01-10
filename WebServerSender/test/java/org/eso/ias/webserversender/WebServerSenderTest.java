package org.eso.ias.webserversender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;

import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.webserversender.WebServerSender;
import org.eso.ias.webserversender.WebServerSender.WebServerSenderListener;
import org.eso.ias.webserversender.WebSocketServerHandler.WebSocketServerListener;

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
	 * Kafka Topic to used in the test
	 */
	private String kafkaTopic = "webserver-sender-test";

	/**
	 * Port to connect to the mock Websocket Server
	 */
	private int port = 8081;

	/**
	 * URI to connect to the mock Websocket Server
	 */
	private String webserverUri = "ws://localhost:" + this.port + "/";

	/**
	 * Number of messages to be used in the test
	 */
	private int messagesNumber;

	/**
	* Array of messages to be used in the test
	*/
	private String[] messages;

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
	private SimpleStringProducer producer;

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WebServerSenderTest.class);

	/**
	* Countdown of the messages sent by the WebServerSender
	*/
	private CountDownLatch numOfMessagesToSend;

	/**
	 * Countdown of the messages received by the Mock Server
	 */
	private CountDownLatch numOfMessagesToReceive;

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
	 * Auxiliary method to start the mock Server
	 */
	private void runServer() throws Exception {
		this.server = new Server(this.port);
        WebSocketHandler wsHandler = new WebSocketServerHandler();
        WebSocketServerListener listener = new WebSocketServerListener(){
        	public void stringEventSent(String event) {
        		numOfMessagesToReceive.countDown();
        		receivedMessages.add(event);
        	};
        };
        WebSocketServerHandler.setListener(listener);
        this.server.setHandler(wsHandler);
		this.server.start();
		logger.info("Server initialized...");
	}

	/**
	 * Auxiliary method to start the WebServerSender
	 */
	private void runSender() throws Exception {
        WebServerSenderListener listener = new WebServerSenderListener(){
        	public void stringEventSent(String event) {
        		numOfMessagesToSend.countDown();
        		sentMessages.add(event);
        	};
        };
		this.webServerSender = new WebServerSender("WebServerSender", this.kafkaTopic, this.webserverUri, listener);
		this.webServerSender.run();
		logger.info("WebServerSender initialized...");
	}

	/**
	 * Test set up
	 */
	@Before
	public void setUp() throws Exception {
		logger.info("Starting Test Initialization");

		this.messagesNumber = 6;
		this.messages = new String[messagesNumber];
		this.messages[0] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[1] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[2] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[3] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[4] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[5] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";

		this.numOfMessagesToReceive = new CountDownLatch(messagesNumber);
		this.numOfMessagesToSend = new CountDownLatch(messagesNumber);

		this.producer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, kafkaTopic, "ProducerTest");
		this.producer.setUp();

		this.runServer();
		this.runSender();
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
		for (int i = 0; i < messagesNumber; i++) {
			// Sends the messages synchronously to get
			// an exception in case of error
			try {
				this.producer.push(messages[i], null, messages[i]);
				this.producer.flush();
				messagesDelivered += 1;
			}
			catch (Exception e) {
				logger.error("Message was not published to the Kafka Queue by Mock Producer");
			}
		}

		logger.info("{} messages delivered", messagesDelivered);

		if (!this.numOfMessagesToSend.await(10, TimeUnit.SECONDS)) {
			for (String str: messages) {
				if (!this.receivedMessages.contains(str)) {
					logger.error("[{}] never sent by the sender",str);
				}
			}
		}

		if (!this.numOfMessagesToReceive.await(10, TimeUnit.SECONDS)) {
			for (String str: messages) {
				if (!this.receivedMessages.contains(str)) {
					logger.error("[{}] never received by the server",str);
				}
			}
		}
		logger.info("{} messages received", this.receivedMessages.size());

		// Assert:
		assertEquals("Some messages were not published in the Kafka Queue by Mock Producer", messagesNumber, messagesDelivered);
		assertEquals("Some messages were not sent by the WebServerSender", messagesNumber, this.sentMessages.size());
		assertEquals("Some messages were not received by the Mock Server", messagesNumber, this.receivedMessages.size());
		logger.info("Test Execution Completed");
	}

	/**
	 * Stop the WebserverSender, Server and Producer after the test is finished
	 */
	@After
	public void tearDown() throws Exception {
		logger.info("Starting Test Finalization");
		this.producer.tearDown();
		this.webServerSender.stop();
		this.server.stop();
		logger.info("Test Finalization Completed");
	}
}
