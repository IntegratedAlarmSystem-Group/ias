package org.eso.ias.webserversender;

import org.apache.commons.cli.*;
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
import org.eso.ias.cdb.pojos.LogLevelDao;
import org.eso.ias.cdb.rdb.RdbReader;
import org.eso.ias.heartbeat.*;
import org.eso.ias.heartbeat.publisher.HbKafkaProducer;
import org.eso.ias.heartbeat.serializer.HbJsonSerializer;
import org.eso.ias.kafkautils.FilteredKafkaIasiosConsumer;
import org.eso.ias.kafkautils.FilteredKafkaIasiosConsumer.FilterIasValue;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.KafkaStringsConsumer.StartPosition;
import org.eso.ias.kafkautils.SimpleKafkaIasiosConsumer.IasioListener;
import org.eso.ias.logging.IASLogger;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.IASValue;
import org.eso.ias.types.IasValueJsonSerializer;
import org.eso.ias.types.IasValueSerializerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The WebServerSender gets IASValues from the copre kafka topic and forwards
 * them to the web server through websockets.
 *
 * Received IASValues are cached before being sent at regular time intervals
 * by a dedicated thread or when the max numer of logs to send has been reached,
 * whatever happens first.
 *
 * The limit of the max number of values is not strict. As logs are received in bounces,
 * they are all aded to the cache and the size of the cache checked later. So the promise
 * is to send the values as soon as the zize of the cache is equal or freater than the
 * maximum allowed number of values.
 *
 * Sending values when the max number of items in cache has been reached does not affect
 * the periodic thread that continues running at his schedule.
 * Afetr sending by max number of items, it can happen that the periodic task
 * is executed shortly after.
 *
 */
@WebSocket(maxTextMessageSize = 65536) // About 1000 IASValues
public class WebServerSender implements IasioListener {

    /**
     *
     * Max size of messages through websockets: while sending
     * messages (IASValues) through websockets they are partitioned in several
     * messages whose max lengthe does not exceeex maxTextMessageSize otherwise
     * websocket disconnects
     *
     * Must match with maxTextMessageSize in @WebSocket
     */
    private static final long maxTextMessageSize = 65535; // About 1000 IASValues

	/**
	 * The identifier of the sender
	 */
	public final String senderID;

	/**
	* IAS Core Kafka Consumer to get messages from the Core
	*/
	private final FilteredKafkaIasiosConsumer kafkaConsumer;

	/**
	 * The name of the topic where webserver senders get
	 * monitor point values and alarms
	 */
	private final String sendersInputKTopicName;

	/**
	 * Default max number of  IASValues to the web server in one single send
	 */
	private static final int DEFAULT_MAX_VALUES_TO_SEND = 10000;

	/**
	 * The property to customize the time interval to send IASValues
	 * to the web server
	 */
	private static final String MAX_VALUES_TO_SEND_PROP_NAME = "org.eso.ias.websenderserver.maxvaluestosend";

	/**
	 * The time interval (msecs) to send IASValues to the web server
	 */
	private static final int MAX_NUM_OF_VALUES_TO_SEND = Integer.getInteger(MAX_VALUES_TO_SEND_PROP_NAME, DEFAULT_MAX_VALUES_TO_SEND);

	/**
	 * Default time interval (msecs) to send IASValues to the web server
	 */
	private static final long DEFAULT_TIME_INTERVAL = 250;

	/**
	 * The property to customize the time interval to send IASValues
	 * to the web server
	 */
	private static final String SEND_THREAD_TIME_INTERVAL_PROP_NAME = "org.eso.ias.websenderserver.time.interval";

	/**
	 * The time interval (msecs) to send IASValues to the web server
	 */
	private static final long TIME_INTERVAL = Long.getLong(SEND_THREAD_TIME_INTERVAL_PROP_NAME,DEFAULT_TIME_INTERVAL);

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
	 * The cache to buffer IASValues read from the kafka topic
	 * The key is the ID of the Monitor point
	 * The value is the IASValue read from kafka topic
	 */
	private Map<String,IASValue<?>> cache = new HashMap<>();

	/**
	 * Signal that a sending to websockets is running to avoid overlaps between
	 * sending by time and sending by max number of items in cache
	 */
	private final AtomicBoolean alreadySendingThroughWebsockets = new AtomicBoolean(false);

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
     * For statistics: the number of IASIOs consumbed from the BSDB in past interval
     */
	private final AtomicLong iasiosReceived = new AtomicLong(0);

    /**
     * For statistics: the number of IASIOs sent to the web server  in past interval
     */
    private final AtomicLong iasiosSentToWebServer = new AtomicLong(0);

    /**
     * The property to set the time interval for the generation of statistics
     * in minutes
     */
    public static final String STATISTIC_TIME_INTERVAL_PROP_NAME = "org.eso.ias.senders.webserver.stats.interval";

    /**
     * The defualt interval to publish statistics in minutes
     */
    public static final long DEFAULT_STATS_TIME_INTERVAL = 10;


    /** The time interval (minutes) to publish sttistics read from the
     * system properties or DEFAULT_STATS_TIME_INTERVAL if not found
     */
    private final long statsTimeInterval = Long.getLong(STATISTIC_TIME_INTERVAL_PROP_NAME,DEFAULT_STATS_TIME_INTERVAL);

    /**
     * The scheduler to publish statistics
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();



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
	 * User java properties
	 */
	Properties props;

	/**
	 * Constructor
	 *
	 * @param senderID Identifier of the WebServerSender
	 * @param kafkaServers Kafka servers URL
	 * @param props the properties to get kafka servers, topic names and webserver uri
	 * @param listener The listenr of the messages sent to the websocket server
	 * @param hbFrequency the frequency of the heartbeat (seconds)
	 * @param hbProducer the sender of HBs
	 * @param acceptedIds The IDs of the IASIOs to consume
	 * @param acceptedTypes The IASTypes to consume
	 * @throws URISyntaxException
	 */
	public WebServerSender(
			String senderID,
			String kafkaServers,
			Properties props,
			WebServerSenderListener listener,
			int hbFrequency,
			HbProducer hbProducer,
			Set<String> acceptedIds,
			Set<IASTypes> acceptedTypes) throws URISyntaxException {
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
		this.props=props;
		this.props.put("group.id", this.senderID + ".kafka.group");
 		sendersInputKTopicName = props.getProperty(IASCORE_TOPIC_NAME_PROP_NAME, KafkaHelper.IASIOs_TOPIC_NAME);
		String wsUri = props.getProperty(WEBSERVER_URI_PROP_NAME, DEFAULT_WEBSERVER_URI);
		String wsPassword = System.getenv("WS_CONNECTION_PASS");
		if (wsPassword != null) {
			wsUri += "?password=" + wsPassword;
		}
		webserverUri = wsUri;
		uri = new URI(webserverUri);
		logger.debug("Websocket connection URI: "+ webserverUri);
		logger.debug("Kafka server: "+ kafkaServers);
		senderListener = Optional.ofNullable(listener);

		logger.debug("*********** acceptedIds: " + Arrays.toString(acceptedIds.toArray()));
		logger.debug("*********** acceptedTypes: " + Arrays.toString(acceptedTypes.toArray()));
		FilterIasValue filter = new FilterIasValue() {
			public boolean accept(IASValue<?> value) {
				assert(value!=null);

				// Locally copy the sets that are immutable and volatile
		        // In case the setFilter is called in the mean time...
				Set<String> acceptedIdsNow = acceptedIds;
				Set<IASTypes> acceptedTypesNow = acceptedTypes;

				boolean acceptedById = acceptedIdsNow.isEmpty() || acceptedIdsNow.contains(value.id);
				boolean acceptedByType = acceptedTypesNow.isEmpty() || acceptedTypesNow.contains(value.valueType);
				return acceptedById || acceptedByType;
			}
		};
		kafkaConsumer = new FilteredKafkaIasiosConsumer(kafkaServers, sendersInputKTopicName, this.senderID, filter);

		if (hbFrequency<=0) {
			throw new IllegalArgumentException("Invalid frequency "+hbFrequency);
		}

		Objects.requireNonNull(hbProducer);
		hbEngine = HbEngine.apply(senderID, HeartbeatProducerType.SINK, hbFrequency, hbProducer);
	}


	/**
	 * Operations performed on connection close
	 *
	 * @param statusCode
	 * @param reason
	 */
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
	    hbEngine.updateHbState(HeartbeatStatus.PARTIALLY_RUNNING);
		logger.info("WebSocket connection closed. status: " + statusCode + ", reason: " + reason);
		socketConnected.set(false);
	    sessionOpt = Optional.empty();
		if (statusCode != 1001) {
			 logger.info("Trying to reconnect");
			 this.connect();
			 hbEngine.updateHbState(HeartbeatStatus.RUNNING);
		 } else {
			 logger.info("The Server is going away");
			 this.shutdown();
		 }
	}

	/**
	 * Operations performed on connection start
	 *
	 * @param session
	 */
	@OnWebSocketConnect
	public void onConnect(Session session) {
	   sessionOpt = Optional.ofNullable(session);
	   this.connectionReady.countDown();
	   socketConnected.set(true);
	   logger.info("WebSocket got connect. remoteAdress: " + session.getRemoteAddress());
		session.getPolicy().setMaxTextMessageBufferSize(1400000);
		session.getPolicy().setMaxTextMessageSize(1500000);
		session.getPolicy().setMaxBinaryMessageBufferSize(1600000);
		session.getPolicy().setMaxBinaryMessageSize(1700000);
   }

	@OnWebSocketMessage
    public void onMessage(String message) {
		notifyListener(message);
    }

	/**
	 * This method receives IASValues published in the BSDB.
	 *
	 * @see {@link IasioListener#iasiosReceived(Collection)}
	 */
	@Override
	public void iasiosReceived(Collection<IASValue<?>> events) {
	    iasiosReceived.addAndGet(events.size());

        synchronized(this) {
        	events.forEach( event -> cache.put(event.id,event));
		}

        if (cache.size()>MAX_NUM_OF_VALUES_TO_SEND) {
        	sendIasios();
		}
    }

    /**
     * Send the passed string of IASValues though the websocket
     *
     * @param str The JSON string to send formatted as a JSON list of IASValues
     * @param iasValuesInString th enuimber of IASValues in the list
     */
    private void sendJsonStringToWebsocket(String str, final int iasValuesInString) {

        sessionOpt.ifPresent( session -> {
            String strToSend=str;
            session.getRemote().sendStringByFuture(strToSend);
            logger.debug("{} IasValues sent" + iasValuesInString);
            this.notifyListener(strToSend);
            iasiosSentToWebServer.addAndGet(iasValuesInString);

        });

    }

	/**
	 * Send the IASValues in the cache to the web server through websockets
	 */
	private void sendIasios() {

		if (alreadySendingThroughWebsockets.getAndSet(true)) {
			// Currently sending do nothing

			return;
		}

		Map<String,IASValue<?>> receivedIasios;
		synchronized(this) {
			receivedIasios = cache;
			cache = new HashMap<>();
		}

		if (!socketConnected.get()) {
			logger.warn("The WebSocket is not connected: discard the events");
			alreadySendingThroughWebsockets.getAndSet(false);
			return;
		}
		if (receivedIasios.isEmpty()) {
			alreadySendingThroughWebsockets.getAndSet(false);
			return;
		}

        int iasValuesInString=0;
        StringBuilder ret = new StringBuilder("[");
		boolean first = true;
		Iterator<IASValue<?>> iterator = receivedIasios.values().iterator();
		while (iterator.hasNext()) {

			IASValue<?> iasValue = iterator.next();
			try {
				String jsonStr =  serializer.iasValueToString(iasValue);


				if (ret.length()+jsonStr.length()+1>=maxTextMessageSize) {
				    // Adding the current  jsonStr is not possible without exceeding the size:
                    // close the JSON string and send what is in the buffer (ret) and delay sending
                    // this jsonStr later
                    ret.append(']');
                    sendJsonStringToWebsocket(ret.toString(),iasValuesInString);
                    // Get ready for next sending
                    ret.delete(0,ret.length());
                    ret.append('[');
                    iasValuesInString=0;
                    first=true;

                }

                if (first) {
                    first = false;
                } else {
                    ret. append(", ");
                }
                iasValuesInString=iasValuesInString+1;
                ret.append(jsonStr);

			} catch (IasValueSerializerException avse){
				logger.error("Error converting the event into a string", avse);
			}
		};
		ret.append("]");
		receivedIasios.clear();

		if (iasValuesInString>0) {
		    sendJsonStringToWebsocket(ret.toString(),iasValuesInString);
        }

		alreadySendingThroughWebsockets.getAndSet(false);
	}

	public void setUp() {
		hbEngine.start();

		// Start the thread to send values though the websocket
		logger.info("Will send values to websockets every {} msecs",TIME_INTERVAL);
		Runnable senderRunnable = new Runnable() {
			@Override
			public void run() {
				sendIasios();
			}
		};
		scheduler.scheduleAtFixedRate(senderRunnable,TIME_INTERVAL,TIME_INTERVAL,TimeUnit.MILLISECONDS);

		if (statsTimeInterval>0) {
		    logger.info("Will publish stats every {} minutes",statsTimeInterval);

		    Runnable statsRunnable = new Runnable() {
                @Override
                public void run() {
                    long msgConsumed = iasiosReceived.getAndSet(0);
                    long msgSent = iasiosSentToWebServer.getAndSet(0);
                    long msgLost = msgConsumed-msgSent;
                    if (msgLost<0) {
                        msgLost=0;
                    }

                    logger.info("Stats: {} IASIOs consumed from BSDB; {} messages sent to though web sockets; {} messages lost",
                            msgConsumed, msgSent,msgLost);
                }
            };

            scheduler.scheduleAtFixedRate(statsRunnable,statsTimeInterval,statsTimeInterval,TimeUnit.MINUTES);
        } else {
		    logger.info("Stats generation disabled");
        }
		connect();
		try {
			kafkaConsumer.setUp(this.props);
			kafkaConsumer.startGettingEvents(StartPosition.END, this);
			logger.info("Kafka consumer starts getting events");
 	    }
 	    catch (Throwable t) {
 	        logger.error("Kafka consumer initialization fails", t);
 	        System.exit(-1);
 	    }
 	    hbEngine.updateHbState(HeartbeatStatus.RUNNING);
	}

	/**
	 * Initializes the WebSocket connection
	 */
	public void connect() {
		try {
			sessionOpt = Optional.empty();
			this.connectionReady = new CountDownLatch(1);
			client = new WebSocketClient();
			client.getPolicy().setMaxTextMessageBufferSize(1000000);
			client.getPolicy().setMaxTextMessageSize(1100000);
			client.getPolicy().setMaxBinaryMessageBufferSize(1200000);
			client.getPolicy().setMaxBinaryMessageSize(1300000);
			client.setMaxTextMessageBufferSize(1400000);
			client.setMaxBinaryMessageBufferSize(1500000);
			client.start();
			client.connect(this, this.uri, new ClientUpgradeRequest());

			if(!this.connectionReady.await(reconnectionInterval, TimeUnit.SECONDS)) {
				logger.warn("The connection with the server is taking too long. Trying again.");
				connect();
			}
			logger.debug("Connection started!");
		}
		catch( Exception e) {
			logger.error("Error on WebSocket connection", e);
			logger.info("Trying to reconnect.");
			connect();
		}
	}

	/**
	 * Shutdown the WebSocket client and Kafka consumer
	 */
	public void shutdown() {
		hbEngine.updateHbState(HeartbeatStatus.EXITING);
		scheduler.shutdown();
		kafkaConsumer.tearDown();
		sessionOpt = Optional.empty();
		try {
			client.stop();
			logger.debug("Connection stopped!");
		}
		catch( Exception e) {
			logger.error("Error on Websocket stop",e);
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
	 * Print the usage string
	 *
	 * @param options The options expected in the command line
	 */
	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "WebServerSender Sender-ID ", options );
	}

	public static void main(String[] args) throws Exception {

		// Use apache CLI for command line parsing
		Options options = new Options();
        options.addOption("h", "help", false, "Print help and exit");
        options.addOption("j", "jcdb", true, "Use the JSON Cdb at the passed path");
		options.addOption(Option.builder("t").longOpt("filter-types").desc("Space separated list of types to send (LONG, INT, SHORT, BYTE, DOUBLE, FLOAT, BOOLEAN, CHAR, STRING, ALARM)").hasArgs().argName("TYPES").build());
		options.addOption(Option.builder("i").longOpt("filter-ids").desc("Space separated list of ids to send").hasArgs().argName("IASIOS-IDS").build());
        options.addOption("x", "logLevel", true, "Set the log level (TRACE, DEBUG, INFO, WARN, ERROR)");

		// Parse the command line
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = null;
        try {
            cmdLine = parser.parse(options, args);
        } catch (Exception e) {
			logger.error("Error parsing the comamnd line: " + e.getMessage());
			printUsage(options);
			System.exit(-1);
        }

		// Get help option
		boolean help = cmdLine.hasOption('h');

		// Get Required Sender ID
		List<String> remaingArgs = cmdLine.getArgList();
        Optional<String> senderId;
        if (remaingArgs.isEmpty()) {
            senderId = Optional.empty();
        } else {
            senderId = Optional.of(remaingArgs.get(0));
        }

		// Show help or error message if Sender ID is not specified
		if (help) {
			printUsage(options);
			System.exit(0);
		}
		if (!senderId.isPresent()) {
			System.err.println("Missing Sender ID");
			printUsage(options);
			System.exit(-1);
		}

		// Get filtered TYPES
		Set<IASTypes> acceptedTypes = new HashSet<>();
		if (cmdLine.hasOption("t")) {
			List<String> typesNames = Arrays.asList(cmdLine.getOptionValues('t'));
			for (String typeName : typesNames) {
				if (typeName.toUpperCase().equals("LONG")) acceptedTypes.add(IASTypes.LONG);
		    	else if (typeName.toUpperCase().equals("INT")) acceptedTypes.add(IASTypes.INT);
		    	else if (typeName.toUpperCase().equals("SHORT")) acceptedTypes.add(IASTypes.SHORT);
		    	else if (typeName.toUpperCase().equals("BYTE")) acceptedTypes.add(IASTypes.BYTE);
		    	else if (typeName.toUpperCase().equals("DOUBLE")) acceptedTypes.add(IASTypes.DOUBLE);
		    	else if (typeName.toUpperCase().equals("FLOAT")) acceptedTypes.add(IASTypes.FLOAT);
		    	else if (typeName.toUpperCase().equals("BOOLEAN")) acceptedTypes.add(IASTypes.BOOLEAN);
		    	else if (typeName.toUpperCase().equals("CHAR")) acceptedTypes.add(IASTypes.CHAR);
		    	else if (typeName.toUpperCase().equals("STRING")) acceptedTypes.add(IASTypes.STRING);
		    	else if (typeName.toUpperCase().equals("ALARM")) acceptedTypes.add(IASTypes.ALARM);
		    	else {
					System.err.println("Unsupported Type");
					printUsage(options);
					System.exit(-1);
				}
			}
			String types = "";
			for (IASTypes type: acceptedTypes) {
				types += "," + type.typeName;
			}
			logger.info("Sender will accept IASIOS of types: [" + types.substring(1) + "]");
		}

		// Get filtered IASIOS ids
		Set<String> acceptedIds = new HashSet<>();
		if (cmdLine.hasOption("i")) {
			acceptedIds = new HashSet<String>(Arrays.asList(cmdLine.getOptionValues('i')));
			String ids = "";
			for (String item: acceptedIds) {
				ids += "," + item;
			}
			logger.info("Sender will accept IASIOS with ids: [" + ids.substring(1) + "]");
		}

		// Get Optional CDB filepath
		CdbReader cdbReader = null;
		if (cmdLine.hasOption("j")) {
			String cdbPath = cmdLine.getOptionValue('j').trim();
            File f = new File(cdbPath);
            if (!f.isDirectory() || !f.canRead()) {
                System.err.println("Invalid file path "+cdbPath);
                System.exit(-3);
            }

            CdbFiles cdbFiles=null;
            try {
                cdbFiles= new CdbJsonFiles(f);
            } catch (Exception e) {
                System.err.println("Error initializing JSON CDB "+e.getMessage());
                System.exit(-4);
            }
            cdbReader = new JsonReader(cdbFiles);
        } else {
			cdbReader = new RdbReader();
        }

		// Read ias configuration from CDB
		Optional<IasDao> optIasdao = cdbReader.getIas();
		cdbReader.shutdown();

		if (!optIasdao.isPresent()) {
			throw new IllegalArgumentException("IAS DAO not fund");
		}

		// Set the log level
		Optional<LogLevelDao> logLvl=null;
		Optional<String> logLevelName = Optional.ofNullable(cmdLine.getOptionValue('x'));
        try {
            logLvl = logLevelName.map(name -> LogLevelDao.valueOf(name));
        } catch (Exception e) {
            System.err.println("Unrecognized log level");
			printUsage(options);
            System.exit(-1);
        }
		Optional<LogLevelDao> logLevelFromIasOpt = Optional.ofNullable(optIasdao.get().getLogLevel());
        IASLogger.setLogLevel(
             logLvl.map(l -> l.toLoggerLogLevel()).orElse(null),
             logLevelFromIasOpt.map(l -> l.toLoggerLogLevel()).orElse(null),
             null);

		// Set HB frequency
		int frequency = optIasdao.get().getHbFrequency();

		// Set serializer of HB messages
		HbMsgSerializer hbSerializer = new HbJsonSerializer();

		// Set kafka server from properties or default
		String kServers=System.getProperty(KAFKA_SERVERS_PROP_NAME);
		if (kServers==null || kServers.isEmpty()) {
			kServers=optIasdao.get().getBsdbUrl();
		}
		if (kServers==null || kServers.isEmpty()) {
			kServers=KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS;
		}

		String id = senderId.get();
		HbProducer hbProd = new HbKafkaProducer(id, kServers, hbSerializer);

		WebServerSender ws=null;
		try {
			ws = new WebServerSender(
				id,
				kServers,
				System.getProperties(),
				null,
				frequency,
				hbProd,
				acceptedIds,
				acceptedTypes);
		} catch (URISyntaxException e) {
			logger.error("Could not instantiate the WebServerSender",e);
			System.exit(-1);
		}
		ws.setUp();
	}

}
