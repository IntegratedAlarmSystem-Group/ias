package org.eso.ias.webserversender;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebServerSender implements KafkaConsumerListener, Runnable {

	/**
	 * Identifier
	 */
	String id;
	
	/**
	 * WebSocket session required to send messages to the Web server
	 */
	public Session session;
	
	/**
	 * IAS Core Kafka Consumer to get messages from the Core
	 */
	SimpleStringConsumer consumer;
	
	/**
	 * Web socket client
	 */
	WebSocketClient client = new WebSocketClient();

	/**
	 * Topic defined to send messages to the IAS Core to the IAS Web Server
	 */
	String kafkaTopic;

	/**
	 * Web Server URI
	 */
	String webserverUri;

	URI uri;

	ClientUpgradeRequest request = new ClientUpgradeRequest();

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WebServerSender.class);
	
	public interface WebServerSenderListener {
		
		public void stringEventSent(String event);
	}

	WebServerSenderListener listener;
	
	/**
	 * Constructor
	 *
   	 * @param id Identifier of the KafkaWebSocketConnector
	 * @param kafkaTopic Topic defined to send messages to the IAS Core to the IAS Web Server
	 * TODO: Add servers to the arguments instead of use the default ones
	 * @param webserverUri 
	 */
	public WebServerSender(String id, String kafkaTopic, String webserverUri, WebServerSenderListener listener) {
    	this.id = id;
    	this.kafkaTopic = kafkaTopic;
		this.webserverUri = webserverUri;
		this.listener = listener;
		this.consumer = new SimpleStringConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, kafkaTopic, this.id);
	}

	/**
	 * Operations performed on connection close
	 *
	 * @param statusCode
	 * @param reason
	 */
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
	   logger.info("WebSocket connection closed:" + statusCode + ", " + reason);
	   this.session = null;
	   this.consumer.tearDown();
	   //System.exit(0); // TODO: Add WebServer reconnection
	}

	/**
	 * Operations performed on connection start
	 *
	 * @param session
	 */
	@OnWebSocketConnect
	public void onConnect(Session session) {
	   logger.info("WebSocket got connect: %s%n",session);
	   this.session = session;
	   try {
	       this.consumer.setUp();
	       this.consumer.startGettingEvents(StartPosition.END, this);
	       logger.info("Starting to listen events\n");
	   }
	   catch (Throwable t) {
	       logger.error("WebSocket couldn't send the message",t);
	   }
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
			this.session.getRemote().sendStringByFuture( event );
			this.notifyListener(event);
		}
		catch (Exception e){
			logger.error("Cannot send messages to the Web Server", e);
		}
	}
	
	/**
	 * Initializes the WebSocket
	 */
	public void run() {
		try {
			this.uri = new URI(this.webserverUri);
			this.client.start();
			this.client.connect(this, this.uri, this.request);
			while(this.session==null) {
				TimeUnit.MILLISECONDS.sleep(100);
			}
			logger.debug("Connection started!");
		}
		catch( InterruptedException e) {
			this.stop();
		}
		catch( Exception e) {
			logger.error("Error on WebSocket connection");
		}
		
	}

	/**
	 * Shutdown the WebSocket client
	 */
	public void stop() {
		try {
			this.client.stop();
			logger.debug("Connection stopped!");
		}
		catch( Exception e) {
			logger.error("Error on Websocket stop");
		}
	}

	/**
	 * Notify the passed string to the listener. 
	 * 
	 * @param strToNotify The string to notify to the listener 
	 */
	protected void notifyListener(String strToNotify) {
		if(listener != null) {
			listener.stringEventSent(strToNotify);
		}
		
	}
	

	
	public static void main(String[] args) throws Exception {

//		WebServerSender ws = new WebServerSender("WebServerSender", "test", "ws://localhost:8000/core/" );
//		ws.run();

	}

}