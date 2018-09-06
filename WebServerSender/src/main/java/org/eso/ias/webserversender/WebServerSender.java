package org.eso.ias.webserversender;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eso.ias.cdb.CdbReader;
import org.eso.ias.cdb.json.CdbFiles;
import org.eso.ias.cdb.json.CdbJsonFiles;
import org.eso.ias.cdb.json.JsonReader;
import org.eso.ias.cdb.pojos.IasDao;
import org.eso.ias.cdb.rdb.RdbReader;
import org.eso.ias.heartbeat.HbEngine;
import org.eso.ias.heartbeat.HbMsgSerializer;
import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.heartbeat.HeartbeatStatus;
import org.eso.ias.heartbeat.publisher.HbKafkaProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaIasiosConsumer;
import org.eso.ias.kafkautils.KafkaIasiosConsumer.IasioListener;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueSerializerException;
import org.eso.ias.types.IASValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebServerSender implements IasioListener {

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
	private Optional<Session> sessionOpt;

	/**
	 * Same as the webserverUri but as a URI object
	 */
	private final URI uri;

	/**
	 * Web socket client
	 */
	private WebSocketClient client;

	/**
	 * Time in seconds to wait before attempt to reconnect
	 */
	private int reconnectionInterval = 2;
	
	/**
	 * The sender of heartbeats
	 */
	private final HbEngine hbEngine;
	
	/**
	 * A flag set to <code>true</code> if the socket is connected
	 */
	public final AtomicBoolean socketConnected = new AtomicBoolean(false);

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
	 * @param kafkaServers Kafka servers URL 
	 * @param props the properties to get kafka servers, topic names and webserver uri
	 * @param listener The listenr of the messages sent to the websocket server
	 * @param hbFrequency the frequency of the heartbeat (seconds)
	 * @param hbProducer the sender of HBs
	 * @throws URISyntaxException 
	 */
	public WebServerSender(
			String senderID, 
			String kafkaServers,
			Properties props, 
			WebServerSenderListener listener,
			int hbFrequency,
			HbProducer hbProducer) throws URISyntaxException {
		Objects.requireNonNull(senderID);
		if (senderID.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty converter ID");
		}
		this.senderID=senderID.trim();
		
		
		Objects.requireNonNull(kafkaServers);
		if (kafkaServers.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid empty kafka servers list");
		}
		this.kafkaServers=kafkaServers.trim();

		Objects.requireNonNull(props);
		sendersInputKTopicName = props.getProperty(IASCORE_TOPIC_NAME_PROP_NAME, KafkaHelper.IASIOs_TOPIC_NAME);
		webserverUri = props.getProperty(WEBSERVER_URI_PROP_NAME, DEFAULT_WEBSERVER_URI);
		uri = new URI(webserverUri);
		logger.debug("Websocket connection URI: "+ webserverUri);
		logger.debug("Kafka server: "+ kafkaServers);
		senderListener = Optional.ofNullable(listener);
		kafkaConsumer = new KafkaIasiosConsumer(kafkaServers, sendersInputKTopicName, this.senderID);
		
		if (hbFrequency<=0) {
			throw new IllegalArgumentException("Invalid frequency "+hbFrequency);
		}
		
		Objects.requireNonNull(hbProducer);
		hbEngine = HbEngine.apply(senderID, hbFrequency, hbProducer);
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
	   socketConnected.set(false);
	   sessionOpt = Optional.empty();
	   if (statusCode != 1001) {
		   logger.info("Trying to reconnect");
		   this.connect();
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
	   sessionOpt = Optional.ofNullable(session);
	   this.connectionReady.countDown();
	   try {
	       kafkaConsumer.startGettingEvents(StartPosition.END, this);
	       logger.info("Starting to listen events");
	   }
	   catch (Throwable t) {
	       logger.error("WebSocket couldn't send the message",t);
	   }
	   socketConnected.set(true);
	}

	@OnWebSocketMessage
    public void onMessage(String message) {
		notifyListener(message);
    }

	/**
	 * This method receives IASValues published in the BSDB.
	 *
	 * @see {@link IasioListener#iasioReceived(IASValue)}
	 */
	@Override
	public synchronized void iasioReceived(IASValue<?> event) {
		if (!socketConnected.get()) {
			// The socket is not connected: discard the event
			return;
		}
		final String value;
		try {
			value = serializer.iasValueToString(event);
		} catch (IasValueSerializerException avse){
			logger.error("Error converting the event into a string", avse);
			return;
		}
		
		sessionOpt.ifPresent( session -> {
			session.getRemote().sendStringByFuture(value);
			logger.debug("Value sent: " + value);
			this.notifyListener(value);
		});
	}
	
	public void setUp() {
		hbEngine.start();
		kafkaConsumer.setUp();
		connect();
	}

	/**
	 * Initializes the WebSocket connection
	 */
	public void connect() {
		try {
			sessionOpt = Optional.empty();
			this.connectionReady = new CountDownLatch(1);
			client = new WebSocketClient();
			client.start();
			client.connect(this, this.uri, new ClientUpgradeRequest());
			if(!this.connectionReady.await(reconnectionInterval, TimeUnit.SECONDS)) {
				logger.info("WebSocketSender could not establish the connection with the server.");
				logger.info("Trying to reconnect");
				connect();
			}
			logger.debug("Connection started!");
		}
		catch( Exception e) {
			logger.error("Error on WebSocket connection", e);
			this.shutdown();
		}
	}

	/**
	 * Shutdown the WebSocket client and Kafka consumer
	 */
	public  void shutdown() {
		hbEngine.updateHbState(HeartbeatStatus.EXITING);
		kafkaConsumer.tearDown();
		sessionOpt = Optional.empty();
		try {
			client.stop();
			logger.debug("Connection stopped!");
		}
		catch( Exception e) {
			logger.error("Error on Websocket stop");
		}
		hbEngine.shutdown();
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
		reconnectionInterval = interval;
	}
	
	/** 
	 * Build the usage message 
	 */
	public static void printUsage() {
		System.out.println("Usage: WebServerSender Sender-ID [-jcdb JSON-CDB-PATH]");
		System.out.println("  -jcdb force the usage of the JSON CDB");
		System.out.println("  Sender-ID: the identifier of the web server sender");
		System.out.println("  JSON-CDB-PATH: the path of the JSON CDB");
	}

	public static void main(String[] args) throws Exception {
		if (args.length!=1 && args.length!=3) {
			printUsage();
			throw new IllegalArgumentException();
		}
		
		String id = args[0].trim();
		if (id.isEmpty()) {
			printUsage();
			throw new IllegalArgumentException("Invalid identifier");
		}
		
		if (args.length==3 && !args[1].equals("-jcdb")) {
			printUsage();
			throw new IllegalArgumentException();
		}
		
		CdbReader reader;
		if (args.length == 3) {
			CdbFiles cdbFiles = new CdbJsonFiles(args[2]);
			reader= new JsonReader(cdbFiles);
		} else {
			reader= new RdbReader();
		}
		
		Optional<IasDao> optIasdao = reader.getIas();
		reader.shutdown();
		
		if (!optIasdao.isPresent()) {
			throw new IllegalArgumentException("IAS DAO not fund");
		}
		int frequency = optIasdao.get().getHbFrequency();
		
		
		// Serializer of HB messages
		HbMsgSerializer hbSerializer = new HbJsonSerializer();
		
		String kServers=System.getProperty(KAFKA_SERVERS_PROP_NAME);
		if (kServers==null || kServers.isEmpty()) {
			kServers=optIasdao.get().getBsdbUrl();
		}
		if (kServers==null || kServers.isEmpty()) {
			kServers=KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS;
		}

		HbProducer hbProd = new HbKafkaProducer(id, kServers, hbSerializer);
		
		WebServerSender ws=null;
		try { 
			ws = new WebServerSender(
				id, 
				kServers,
				System.getProperties(), 
				null,
				frequency,
				hbProd);
		} catch (URISyntaxException e) {
			logger.error("Could not instantiate the WebServerSender",e);
			System.exit(-1);
		}
		ws.setUp();
	}

}
