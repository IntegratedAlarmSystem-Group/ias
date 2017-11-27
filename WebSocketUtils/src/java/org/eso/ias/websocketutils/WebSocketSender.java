package org.eso.ias.websocketutils;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketSender implements KafkaConsumerListener {
	
	/**
	 * Web socket client
	 */
	WebSocketClient client = new WebSocketClient();
	
	/**
	 * Custom socket  
	 */
	SimpleCustomSocket socket = new SimpleCustomSocket();
	
	/**
	 * Topic defined to send messages to the IAS Core to the IAS Web Server
	 */
	String KafkaTopic = "test";
	
	/**
	 * Web Server URI
	 */
	String webserverUri = "ws://localhost:8765/stream/";
	
	URI uri;
	
	ClientUpgradeRequest request = new ClientUpgradeRequest();
	
	/**
	 * WebSocket session required to send messages to the Web server
	 */
	Session session;
	
	/**
	 * IAS Core Kafka Consumer to get messages from the Core
	 */
	SimpleStringConsumer consumer;
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WebSocketSender.class);

	
	/**
	 * Initializes the WebSocket and set the socket session to send the messages
	 * 
	 * @throws Exception
	 */
	public void setUp() throws Exception {
		this.uri = new URI(this.webserverUri);
		this.client.start();
	    this.client.connect(this.socket, this.uri, this.request);
	    logger.info("Connecting to : %s%n", uri);
	    while(this.socket.session == null) {
	    	TimeUnit.MILLISECONDS.sleep(100);
	    }
	    this.session = this.socket.session;
	}
	
	
	/**
	 * Run the KafkaConsumer Listener method to get events and start to send them to the WebServer
	 * 
	 * @throws Exception
	 */
	public void run() throws Exception {
	    consumer = new SimpleStringConsumer(SimpleStringProducer.DEFAULT_BOOTSTRAP_SERVERS, this.KafkaTopic, this);
		consumer.setUp();		
		consumer.startGettingEvents(StartPosition.END);
		logger.info("Starting to listen events\n");	       
	}
	
	
	/**
	 * This method could get notifications for messages
	 * published before depending on the log and offset
	 * retention times. Therefore it discards the strings
	 * not published by this test
	 * @throws Exception 
	 *  
	 * @see org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener#stringEventReceived(java.lang.String)
	 */
	@Override
	public synchronized void stringEventReceived(String event) {
		try {
			this.session.getRemote().sendStringByFuture( "{\"stream\": \"core\", \"payload\": {\"text\": \""+ event +"\"}}" );
		}
		catch (Exception e){
			logger.error("Cannot send messages to the Web Server", e);
		}	
	}

	
	public static void main(String[] args) throws Exception {
		
		WebSocketSender ws = new WebSocketSender();
		ws.setUp();
		ws.run();
		
	}

}
