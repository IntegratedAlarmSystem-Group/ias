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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;



public class WebServerSenderTest {
	private WebServerSender webServerSender;
	private String[] messages;
	private int messagesNumber;
	private Thread senderThread;
	private Thread serverThread;
	private Thread counterThread;

	
	private final CountDownLatch endTest = new CountDownLatch(1);
	
	/**
	 * The list of strings received by the server.
	 * <P>
	 * It contains only the strings that this test produced
	 */
	private final List<String> receivedStrings = Collections.synchronizedList(new ArrayList<>());

	
	public String formatTimestamp(long currentDateMillis) {
		Date currentDate = new Date(currentDateMillis);
		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		String date = dateFormat.format(currentDate);
		
		return date;
	}
	
//
//	public void runProducer() throws KafkaUtilsException {
//		SimpleStringProducer producer = new SimpleStringProducer("localhost:9092", "test", "PID1");
//		producer.setUp();
//
//		for (int i = 0; i < messagesNumber; i++) {
//			try {
//				Thread.sleep(100);
//				String msg = messages[i] + Long.toString(System.currentTimeMillis()) + "}";
//				producer.push(msg, null, msg);
//				producer.flush();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
	public void runWebServerSender(String kafkaTopic, String webserverUri) throws InterruptedException {
		webServerSender = new WebServerSender("WebServerSender", kafkaTopic, webserverUri);
		senderThread = new Thread() {
			public void run() {
				// Initialize Sender
				try {
					webServerSender.run();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				
				// Check constantly if the thread is interrupted
				while(!Thread.currentThread().isInterrupted()){
					try {
						Thread.sleep(500);
					}
					catch( InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				
				// Stop the sender before die
				webServerSender.stop();
				try {
					endTest.wait(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		System.out.println("*************" + formatTimestamp(System.currentTimeMillis()) + "*************** Initializing sender!!");
		
		// Initialize the thread
		senderThread.start();
		
		// Wait 5 seconds and then interrupt it
		endTest.await(5, TimeUnit.SECONDS);
		senderThread.interrupt();
		
		System.out.println("*************" + formatTimestamp(System.currentTimeMillis()) + "*************** After sender!!");
	}
	
//	public void initCounter(int millis) throws InterruptedException {
//		System.out.println("*************"  + formatTimestamp(System.currentTimeMillis()) + "********** Initializing Counter!!");	
//		try {
//			TimeUnit.MILLISECONDS.sleep(millis);
//			endTest.countDown();
//		}
//		catch(Exception e) {
//			e.printStackTrace();
//		}
//		System.out.println("*************"  + formatTimestamp(System.currentTimeMillis()) + "********* Counter Finished!!");	
//	}

	public void runWebsocketServer() throws Exception {
		serverThread = new Thread() {
			public void run() {
				
				// Initialize the server
				try {
					Server server = new Server(8080);
			        WebSocketHandler wsHandler = new WebSocketHandler() {
			        	
			            @Override
			            public void configure(WebSocketServletFactory factory) {
			                factory.register(MyWebSocketHandler.class);
			            }

			        };
			        server.setHandler(wsHandler);
			        server.start();
				}
				catch( Exception e) {
					Thread.currentThread().interrupt();
				}
				
				// Check constantly if the thread is interrupted
				// TODO:  (Duplicated code!)
				while(!Thread.currentThread().isInterrupted()){
					try {
						Thread.sleep(1);
					}
					catch( InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		};
		System.out.println("*************" + formatTimestamp(System.currentTimeMillis()) + "*************** Initializing server!!");
        serverThread.start();
	}

	
//  Uncomment to init server here!	
//	@Before
//	public void setUp() throws Exception {
//		this.runWebsocketServer();
//	}
//	
//	@After
//	public void tearDown() throws Exception {
//		serverThread.interrupt();
//		System.out.println("*************" + formatTimestamp(System.currentTimeMillis()) + "*************** Server interrupted!!");
//
//	}

	@Test
	public void testWebServerSender() throws Exception {
//		messagesNumber = 6;
//		this.messages = new String[messagesNumber];
//		this.messages[0] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
//		this.messages[1] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
//		this.messages[2] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
//		this.messages[3] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
//		this.messages[4] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
//		this.messages[5] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		String kafkaTopic = "test";
		String webserverUri = "ws://localhost:8080/";
		
		this.runWebServerSender(kafkaTopic, webserverUri);
		System.out.println("\n\n****************************Test Ended, Received Strings!");	
		
//		System.out.println("Strings received= " + receivedStrings.size());
//		receivedStrings.forEach(str -> System.out.println(str));

	}
	
	public static void main(String [] args) throws Exception {
		System.out.println("\n*******************************************\n*****************************************");
		WebServerSenderTest test = new WebServerSenderTest();
		test.testWebServerSender();
	}
}
