package org.eso.ias.webserversender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eso.ias.webserversender.WebServerSender;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eso.ias.kafkautils.KafkaUtilsException;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class WebServerSenderTest {
	private WebServerSender webServerSender;
	private String[] messages;
	private int messagesNumber;
	private Thread sender;
	private Thread counter;
	
	private final CountDownLatch closeLatch = new CountDownLatch(1);
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
		sender = new Thread() {
			public void run() {
				try {
					webServerSender.run();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		};
		System.out.println("****************************Initializing sender!!");
		sender.start();
		System.out.println("****************************After sender!!");
		TimeUnit.MILLISECONDS.sleep(10000);
		closeLatch.countDown();
	}
	
//	public void initCounter(int millis) throws InterruptedException {
//		counter = new Thread() {
//			public void run() {
//				try {
//				TimeUnit.MILLISECONDS.sleep(millis);
//				closeLatch.countDown();
//				sender.join();
//				}
//				catch(Exception e) {
//					e.printStackTrace();
//				}
//			}
//		};
//		System.out.println("***********************Initializing Counter!!");
//		counter.start();
//		
//	}
//
//	public void runWebsocketServer() throws Exception {
//		Server server = new Server(8080);
//        WebSocketHandler wsHandler = new WebSocketHandler() {
//            @Override
//            public void configure(WebSocketServletFactory factory) {
//                factory.register(MyWebSocketHandler.class);
//            }
//        };
//        server.setHandler(wsHandler);
//        server.start();
//        server.join();
//	}
	
//	@Before
//	public void setUp() throws Exception {
//		this.runWebsocketServer();
//	}

	@Test
	public void testWebServerSender() throws Exception {
		messagesNumber = 6;
		this.messages = new String[messagesNumber];
		this.messages[0] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		this.messages[1] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		this.messages[2] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		this.messages[3] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		this.messages[4] = "{\"value\":\"SET\",\"mode\":\"OPERATIONAL\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		this.messages[5] = "{\"value\":\"CLEARED\",\"mode\":\"MAINTENANCE\",\"iasValidity\":\"RELIABLE\",\"id\":\"AlarmType-ID\",\"fullRunningId\":\"(Monitored-System-ID:MONITORED_SOFTWARE_SYSTEM)@(plugin-ID:PLUGIN)@(Converter-ID:CONVERTER)@(AlarmType-ID:IASIO)\",\"valueType\":\"ALARM\",\"tStamp\":";
		String kafkaTopic = "test";
		String webserverUri = "ws://localhost:8080/";
//		this.runWebsocketServer();
		this.runWebServerSender(kafkaTopic, webserverUri);
//		this.initCounter(1000);
		//this.runProducer();
		//assertEquals(1, 1);
	}
	
	public static void main(String [] args) throws Exception {
		System.out.println("\n*******************************************\n*****************************************");
		WebServerSenderTest test = new WebServerSenderTest();
		test.testWebServerSender();
	}
}
