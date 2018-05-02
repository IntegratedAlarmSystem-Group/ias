package org.eso.ias.webserversender;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
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
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IASValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebServerSender implements IasioListener, Runnable {

	/**
	 * The identifier of the sender
	 */
	public final String senderID;

	/**
	 * IAS Core Kafka Consumer to get messages from the Core
	 */
	private final KafkaIasiosConsumer kafkaConsumer;

	/**
	 * The name of the topic where webserver senders get
	 * monitor point values and alarms
	 */
	private final String sendersInputKTopicName;

	/**
	 * The name of the java property to get the name of the
	 * topic where the ias core push values
	 */
	private static final String IASCORE_TOPIC_NAME_PROP_NAME = "org.eso.ias.senders.kafka.inputstream";

	/**
	 * The list of kafka servers to connect to
	 */
	private final String kafkaServers;

	/**
	 * The name of the property to pass the kafka servers to connect to
	 */
	private static final String KAFKA_SERVERS_PROP_NAME = "org.eso.ias.senders.kafka.servers";

	/**
	 * Web Server host as String
	 */
	private final String webserverUri;

	/**
	 * The name of the property to pass the webserver host to connect to
	 */
	private static final String WEBSERVER_URI_PROP_NAME = "org.eso.ias.senders.webserver.uri";

	/**
	 * Default webserver host to connect to
	 */
	private static final String DEFAULT_WEBSERVER_URI = "ws://localhost:8000/core/";

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
	 * WebSocket session required to send messages to the Web server
	 */
	private static Session session;

	/**
	 * Same as the webserverUri but as a URI object
	 */
	private static URI uri;

	/**
	 * Web socket client
	 */
	private static WebSocketClient client;

	/**
	 * Time in seconds to wait before attempt to reconnect
	 */
	private static int reconnectionInterval = 2;

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
	private final Optional<WebServerSenderListener> senderListener;

	/**
	 * Count down to wait until the connection is established
	 */
	private CountDownLatch connectionReady;

	/**
	 * Constructor
	 *
   * @param senderID Identifier of the WebServerSender
	 * @param props the properties to get kafka servers, topic names and webserver uri
	 * @param listener The listener of the messages sent to the websocket server
	 */
	public WebServerSender(String senderID, Properties props, WebServerSenderListener listener) {
		Objects.requireNonNull(senderID);
		if (senderID.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty converter ID");
		}
		this.senderID=senderID.trim();

		Objects.requireNonNull(props);
		kafkaServers = props.getProperty(KAFKA_SERVERS_PROP_NAME,KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS);
		sendersInputKTopicName = props.getProperty(IASCORE_TOPIC_NAME_PROP_NAME, KafkaHelper.IASIOs_TOPIC_NAME);
		webserverUri = props.getProperty(WEBSERVER_URI_PROP_NAME, DEFAULT_WEBSERVER_URI);
		logger.debug("Websocket connection URI: "+ webserverUri);
		logger.debug("Kafka server: "+ kafkaServers);
		senderListener = Optional.ofNullable(listener);
		kafkaConsumer = new KafkaIasiosConsumer(kafkaServers, sendersInputKTopicName, this.senderID);
		kafkaConsumer.setUp();
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
	       kafkaConsumer.startGettingEvents(StartPosition.END, this);
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
			if (this.session != null) {
				this.session.getRemote().sendStringByFuture(value);
				logger.debug("Value sent: " + value);
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
			this.uri = new URI(webserverUri);
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
		kafkaConsumer.tearDown();
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
		senderListener.ifPresent(listener -> listener.stringEventSent(strToNotify));
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
		WebServerSender ws = new WebServerSender("WebServerSender", System.getProperties(), null);
		ws.run();
	}

}
