package org.eso.ias.plugin.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.plugin.Plugin;
import org.eso.ias.plugin.PluginException;
import org.eso.ias.plugin.Sample;
import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.types.IASTypes;
import org.eso.ias.types.OperationalMode;
import org.eso.ias.utils.ISO8601Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A plugin that gets monitor points and alarms 
 * from a UDP socket.
 * 
 * Strings received from the socket are pushed in the {@link #receivedStringQueue}
 * queue and processed by a dedicated thread to decouple reading 
 * from processing and better cope with spikes.
 * 
 * @author acaproni
 *
 */
public class UdpPlugin implements Runnable {
	
	/**
	 * The plugin to filter and send data to the 
	 * BSDB
	 */
	private final Plugin plugin; 
	
	/**
	 * The UDP socket
	 */
	private final DatagramSocket udpSocket;
	
	/**
	 * The thread getting strings from the UDP socket
	 * and pushing them in the buffer
	 */
	private volatile Thread udpRecvThread;
	
	/**
	 * The thread getting strings from the buffer and 
	 * sending them to the plugin
	 */
	private volatile Thread stringProcessrThread;
	
	/**
	 * Signal the thread to terminate
	 */
	private final AtomicBoolean terminateThread = new AtomicBoolean(false);
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(UdpPlugin.class);
	
	/**
	 * The latch to be notified about termination
	 */
	private final CountDownLatch done = new CountDownLatch(1);
	
	/**
	 * The max size of the buffer
	 */
	public static final int receivedStringBufferSize = 2048;
	
	/**
	 * The mapper to convert received strings into {@link MessageDao}
	 */
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	/**
	 * The buffer of strings received from the socket
	 */
	private final LinkedBlockingDeque<String> receivedStringQueue = new LinkedBlockingDeque<>(receivedStringBufferSize);
	
	/**
	 * Constructor
	 * 
	 * @param config the configuration of the plugin
	 * @param sender the publisher of monitor points to the BSDB
	 * @param hbProducer the sender of heartbeats
	 * @param udpPort the UDP port
	 * @throws SocketException in case of error creating the UDP socket
	 */
	public UdpPlugin(
			PluginConfig config,
			MonitorPointSender sender,
			HbProducer hbProducer,
			int udpPort) throws SocketException {
		Objects.requireNonNull(config);
		Objects.requireNonNull(sender);
		Objects.requireNonNull(hbProducer);
		plugin = new Plugin(config,sender,hbProducer);
		
		if (udpPort<1024) {
			throw new IllegalArgumentException("Invalid UDP port: "+udpPort);
		}
		udpSocket = new DatagramSocket(udpPort);
	}

	/**
	 * The main to start the plugin
	 */
	public static void main(String[] args) {
		

	}

	@Override
	public void run() {
		// The buffer
		byte[] buffer = new byte[2048];
		logger.debug("UDP loop thread started");
		// The loop to get monitor from the socket 
		while (!terminateThread.get()) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				udpSocket.receive(packet);
			} catch (Exception e) {
				if (!terminateThread.get()) {
					logger.error("Error receiving data from UDP socket.",e);
				}
				continue;
			}
			String receivedString = new String(packet.getData());
			logger.debug("Packet of size {} received from {}:{}: [{}]",
					receivedString.length(),
					packet.getAddress(),
					packet.getPort(),
					receivedString);
			// Put the string in the buffer
			if (receivedString.isEmpty()) {
				logger.warn("Got an empty string from the socket?!?");
				continue;
			}
			boolean addedToQueue = receivedStringQueue.offer(receivedString);
			if (!addedToQueue) {
				logger.warn("Queue full: string rejected: [{}]",receivedString);
			} else {
				logger.debug("String [{}] pushed in buffer (size of buffer ={})",
						receivedString,
						receivedStringQueue.size());
			}
		}
		done.countDown();
		logger.debug("UDP loop thread terminated");
	}
	
	/**
	 * Submit a value received from the socket to the
	 * plugin
	 * 
	 * @param str
	 */
	private void submitValue(String str) {
		if (str==null || str.isEmpty()) {
			throw new IllegalArgumentException("Invalid value from socket");
		}
		logger.debug("Submitting [{}] to the plugin library",str);
		
		MessageDao message;
		try { 
			message = MAPPER.readValue(str, MessageDao.class);
		} catch (Exception e) {
			logger.error("Exception parsing JSON string [{}]: value lost",str,e);
			return;
		}
		Object value;
		try { 
			value = convertStringToObject(message.getValue(),message.getValueType());
		} catch (PluginException pe) {
			logger.error("Exception building the object of type [{}] and value[{}]: value lost",
					message.getValue(),
					message.getValueType(),
					pe);
			return;
		}
		
		if (!Objects.isNull(message.getOperMode()) && !message.getOperMode().isEmpty()) {
			try {
				OperationalMode mode = OperationalMode.valueOf(message.getOperMode());
				plugin.setOperationalMode(message.getMonitorPointId(), mode);
			} catch (PluginException e) {  
				// This exception is thrown by plugin.setOperationalMode
				logger.error("Error setting the operational mode {} for  monitor point {}",
						message.getOperMode(),
						message.getMonitorPointId());
			}catch (Exception e) {
				logger.error("Error decoding operational mode {} for  monitor point {}",
						message.getOperMode(),
						message.getMonitorPointId());
			}
			
		}
		
		long timestamp;
		try { 
			timestamp = ISO8601Helper.timestampToMillis(message.getTimestamp());
		} catch (Exception e) {
			logger.error("Exception parsing te timestamp [{}]: using actual time",message.getTimestamp(),e);
			timestamp=System.currentTimeMillis();
		}
		
		Sample sample = new Sample(value,timestamp);
		try {
			plugin.updateMonitorPointValue(message.getMonitorPointId(), sample);
		} catch (Exception e) {
			logger.error("Exception adding the sample [{}] to the plugin: value lost",message.getMonitorPointId(),e);
		}
	}
	
	/**
	 * Parse the passed string of the give type into a java object
	 * 
	 * @param value the string representation of the value
	 * @param valueType the type of the value
	 * @return the java object for the give value and type
	 * @throws PluginException in case of error building the object
	 */
	private Object convertStringToObject(String value, String valueType) throws PluginException {
		if (value==null || value.isEmpty()) {
			throw new PluginException("Invalid value string to parse");
		}
		if (valueType==null || valueType.isEmpty()) {
			throw new PluginException("Invalid value type");
		}
		
		IASTypes iasType;
		try { 
			iasType = IASTypes.valueOf(valueType);
		} catch (Exception e) {
			throw new PluginException("Unrecognized/Unsupported value type "+valueType);
		}
		try {
			return iasType.convertStringToObject(value);
		} catch (Exception e) {
			throw new PluginException("Exception converting "+value+" to an object of type "+iasType,e);
		}
		
	}
	
	/**
	 * Starts the UdpPlugin
	 * 
	 * @return the latch signaling the termination of the thread
	 * @throws PluginException in case of error running the plugin  
	 */
	public CountDownLatch setUp() throws PluginException {
		logger.debug("Starting the plugin");
		try {
			plugin.start();
		} catch (PublisherException pe) {
			throw new PluginException("Error starting the plugin",pe);
		}
		logger.debug("Starting the string processor loop");
		stringProcessrThread = Plugin.getThreadFactory().newThread(new Runnable() {
			public void run() {
				logger.debug("String processor thread started");
				while (!terminateThread.get()) {
					String strToInject=null;
					try {
						strToInject= receivedStringQueue.take();
					} catch (InterruptedException ie) {
						if (!terminateThread.get()) {
							logger.warn("Interrupted",ie);
						}
						continue;
					}
					try {
						if (strToInject!=null && !strToInject.isEmpty()) {
							submitValue(strToInject);
						}
					} catch (Exception e) {
						logger.warn("Error processing [{}]: ignored",strToInject,e);
					}
				}
			}
		});
		stringProcessrThread.start();
		
		logger.debug("Starting the UDP loop");
		udpRecvThread = Plugin.getThreadFactory().newThread(this);
		udpRecvThread.start();
		// Adds the shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				shutdown();
			}
		}, "UdpPlugin shutdown hook"));
		logger.info("Started.");
		return done;
	}
	
	/**
	 * Shuts down the the thread and closes the plugin
	 */
	public void shutdown() {
		logger.debug("Shutting down the UDP loop");
		terminateThread.set(true);
		if (udpRecvThread!=null) {
			udpRecvThread.interrupt();
		}
		if (stringProcessrThread!=null) {
			stringProcessrThread.interrupt();
		}
		boolean terminatedInTime;
		try {
			terminatedInTime = done.await(2, TimeUnit.SECONDS);
			if (!terminatedInTime) {
				logger.warn("The UDP loop did not temrinate in time");
			}
		} catch (InterruptedException e) {
			logger.warn("Interrupetd while waiting for thread termination",e);
		}
		logger.debug("Closing the UDP socket");
		udpSocket.close();
		logger.debug("Shutting down the plugin");
		plugin.shutdown();
		logger.info("Cleaned up.");
	}

}
