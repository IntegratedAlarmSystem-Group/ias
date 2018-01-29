package org.eso.ias.webserversender;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosConsumer;
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
import org.eso.ias.prototype.input.java.IasValueJsonSerializer;
import org.eso.ias.prototype.input.java.IASValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebServerSender implements IasioListener, Runnable {

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
	KafkaIasiosConsumer consumer;

	/**
	 * Web socket client
	 */
	WebSocketClient client;

	/**
	 * Topic defined to send messages to the IAS Core to the IAS Web Server
	 */
	String kafkaTopic;

	/**
	 * Web Server URI as String
	 */
	String webserverUri;

	/**
	 * Same as the webserverUri but as a URI object
	 */
	URI uri;
	
	/**
	 * Time in seconds to wait before attempt to reconnect 
	 */
	int reconnectionInterval = 2;

	/**
	 * The serializer/deserializer to convert the string
	 * received by the BSDB in a IASValue
	*/
	private final IasValueJsonSerializer serializer = new IasValueJsonSerializer();

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WebServerSender.class);

	/**
	 * The interface of the listener to be notified of Strings received
	 * by the WebServer sender.
	 */
	public interface WebServerSenderListener {

		public void stringEventSent(String event);
	}

	/**
	 * The listener to be notified of Strings received
	 * by the WebServer sender.
	 */
	WebServerSenderListener listener;

	/**
	 * Count down to wait until the connection is established
	 */
	private CountDownLatch connectionReady;

	/**
	 * Constructor
	 *
   	 * @param id Identifier of the KafkaWebSocketConnector
	 * @param kafkaTopic Topic defined to send messages to the IAS Core to the IAS Web Server
	 * @param webserverUri
	 * @param listener The listener of the messages received by the server
	 */
	public WebServerSender(String id, String kafkaTopic, String webserverUri, WebServerSenderListener listener) {
    	this.id = id;
    	this.kafkaTopic = kafkaTopic;
		this.webserverUri = webserverUri;
		this.listener = listener;
		this.consumer = new KafkaIasiosConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, kafkaTopic, this.id);
		this.consumer.setUp();
	}

	/**
	 * Constructor
	 *
   	 * @param id Identifier of the KafkaWebSocketConnector
	 * @param webserverUri
	 * @param listener The listener of the messages received by the server
	 */
	public WebServerSender(String id, String webserverUri, WebServerSenderListener listener) {
    	this.id = id;
		this.webserverUri = webserverUri;
		this.listener = listener;
		this.consumer = new KafkaIasiosConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, KafkaHelper.IASIOs_TOPIC_NAME, this.id);
		this.consumer.setUp();
	}

	/**
	 * Operations performed on connection close
	 *
	 * @param statusCode
	 * @param reason
	 */
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
	   logger.info("WebSocket connection closed. status: " + statusCode + ", reason: " + reason);
	   this.session = null;
	   if (statusCode != 1001) {
		   logger.info("Trying to reconnect");
		   this.run();
	   }
	   else {
		   logger.info("WebServerSender was stopped");
	   }
	}

	/**
	 * Operations performed on connection start
	 *
	 * @param session
	 */
	@OnWebSocketConnect
	public void onConnect(Session session) {
	   logger.info("WebSocket got connect. remoteAdress: " + session.getRemoteAddress());
	   this.session = session;
	   this.connectionReady.countDown();
	   try {
	       this.consumer.startGettingEvents(StartPosition.END, this);
	       logger.info("Starting to listen events");
	   }
	   catch (Throwable t) {
	       logger.error("WebSocket couldn't send the message",t);
	   }
	}

	@OnWebSocketMessage
    public void onMessage(String message) {
		notifyListener(message);
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
	public synchronized void iasioReceived(IASValue<?> event) {
		try {
			String value = serializer.iasValueToString(event);
			logger.info("Attempting to send value: "+ value);
			if (this.session != null) {
				this.session.getRemote().sendStringByFuture(value);
				logger.info("Value sent");
				this.notifyListener(value);
			}
			else {
				logger.debug("No server available to send message: " + value);
			}
		}
		catch (Exception e){
			logger.error("Error sending message to the Web Server", e);
		}
	}


	/**
	 * Initializes the WebSocket connection
	 */
	public void run() {
		try {
			this.uri = new URI(this.webserverUri);
			this.session = null;
			this.connectionReady = new CountDownLatch(1);
			this.client = new WebSocketClient();
			this.client.start();
			this.client.connect(this, this.uri, new ClientUpgradeRequest());
			if(!this.connectionReady.await(this.reconnectionInterval, TimeUnit.SECONDS)) {
				logger.info("WebSocketSender could not establish the connection with the server.");
				logger.info("Trying to reconnect");
				this.run();
			}
			logger.debug("Connection started!");
		}
		catch( Exception e) {
			logger.error("Error on WebSocket connection", e);
			this.stop();
		}
	}

	/**
	 * Shutdown the WebSocket client and Kafka consumer
	 */
	public void stop() {
		this.session = null;
		this.consumer.tearDown();
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
	
	/**
	 * Set the time to wait before attempt to reconnect
	 * 
	 * @param interval time in seconds
	 */
	public void setReconnectionInverval(int interval) {
		this.reconnectionInterval = interval;
	}

	public static void main(String[] args) throws Exception {
		WebServerSender ws = new WebServerSender("WebServerSender", "ws://localhost:8000/core/", null);
		ws.run();	
	}

}
