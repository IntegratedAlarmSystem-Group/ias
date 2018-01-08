package org.eso.ias.webserversender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.eso.ias.webserversender.WebServerSender;
import org.eso.ias.webserversender.WebServerSender.WebServerSenderListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;



public class WebServerSenderTest implements WebServerSenderListener {
	private WebServerSender webServerSender;
	
	private String[] messages;
	
	private int messagesNumber;
	
	private Thread senderThread;
	
	private Thread serverThread;
	
	/**
	 * The producer to publish events
	 */
	private SimpleStringProducer producer;
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WebServerSenderTest.class);

	/**
	 * The number of events to wait for
	 */
	private CountDownLatch numOfMessagesToReceive;
	
	/**
	 * The list of strings received by the server.
	 * <P>
	 * It contains only the strings that this test produced
	 */
	private final List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

	
//	public void runWebServerSender(String kafkaTopic, String webserverUri) throws InterruptedException {
//		webServerSender = new WebServerSender("WebServerSender", kafkaTopic, webserverUri);
//		senderThread = new Thread(webServerSender);
//		senderThread.start();
//		logger.info("WebServerSender initialized");
//		numOfMessagesToReceive.await(5, TimeUnit.SECONDS);
//	}
	
//	public void runWebsocketServer() throws Exception {
//		serverThread = new Thread() {
//			public void run() {
//				Server server = new Server(8080);
//				WebSocketHandler wsHandler = new WebSocketHandler() {
//		        	
//		            @Override
//		            public void configure(WebSocketServletFactory factory) {
//		                factory.register(MyWebSocketHandler.class);
//		            }
//		            
//		            @OnWebSocketMessage
//		            public synchronized void onMessage(String message) {
//		                logger.info("Message received: " + message);
//		                receivedMessages.add(message);
//		                numOfMessagesToReceive.countDown();
//		            }
//
//		        };
//		        server.setHandler(wsHandler);
//				
//				// Initialize the server
//				try {
//			        server.start();
//				}
//				catch( Exception e) {
//					Thread.currentThread().interrupt();
//				}
//				
//				// Check constantly if the thread is interrupted
//				// TODO:  (Duplicated code!)
//				while(!Thread.currentThread().isInterrupted()){
//					try {
//						Thread.sleep(500);
//					}
//					catch( InterruptedException e) {
//						Thread.currentThread().interrupt();
//					}
//				}
//				
//				try {
//					server.stop();
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		};
//
//        serverThread.start();
//		logger.info("WebSocket server initialized");
//	}

	
	public synchronized void stringEventSent( String event) {
		logger.info("\n*******************\n*************" + event);
		numOfMessagesToReceive.countDown();
		receivedMessages.add(event);
	}
	
	@Before
	public void setUp() throws Exception {
		String kafkaTopic = "test";
		String webserverUri = "ws://localhost:8081/";

		
		logger.info("Initializing...");
		messagesNumber = 6;
		numOfMessagesToReceive = new CountDownLatch(messagesNumber);
		
		producer = new SimpleStringProducer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, kafkaTopic, "ProducerTest");
		producer.setUp();
		
		webServerSender = new WebServerSender("WebServerSender", kafkaTopic, webserverUri, null);
		webServerSender.run();
		logger.info("WebServerSender initialized");
		
//		this.runWebServerSender(kafkaTopic, webserverUri);
		logger.info("Initialized.");
	}
	
	@After
	public void tearDown() throws Exception {
		logger.info("Closing...");
		producer.tearDown();
		logger.info("Closed after processing {} messages",receivedMessages.size());
	}

	@Test
	public void testWebServerSender() throws Exception {
			
//		this.runWebServerSender(kafkaTopic, webserverUri);
		
		int messagesDelivered = 0;	
		this.messages = new String[messagesNumber];
		this.messages[0] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[1] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[2] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[3] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[4] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		this.messages[5] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":1000}";
		
		logger.info("Start to send messages");
		for (int i = 0; i< messagesNumber; i++) {
			// Sends the messages synchronously to get
			// an exception in case of error
			try {
				producer.push(messages[i], null, messages[i]);
				producer.flush();
				messagesDelivered += 1;
			}
			catch (Exception e) {
				logger.error("Message was not deliver");
			}
		}

		logger.info("{} messages delivered", messagesDelivered);
		assertEquals(messagesDelivered, messagesNumber);
		
		if (!numOfMessagesToReceive.await(10, TimeUnit.SECONDS)) {
//			logger.error("TIMEOUT received "+receivedMessages.size()+" instead of "+nrOfStrings);
			for (String str: messages) {
				if (!receivedMessages.contains(str)) {
					logger.error("[{}] never received",str);
				}
			}
		}
		senderThread.interrupt();
	}
	
	public static void main(String [] args) throws Exception {
		WebServerSenderTest test = new WebServerSenderTest();
		test.testWebServerSender();
	}
}
