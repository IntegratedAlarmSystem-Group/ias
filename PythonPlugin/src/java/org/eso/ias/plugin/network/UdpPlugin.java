package org.eso.ias.plugin.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eso.ias.heartbeat.HbProducer;
import org.eso.ias.plugin.Plugin;
import org.eso.ias.plugin.PluginException;
import org.eso.ias.plugin.config.PluginConfig;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A plugin that gets monitor points and alarms 
 * from a UDP socket.
 * 
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
	 * The number of the UDP port
	 */
	private final int udpPort;
	
	/**
	 * The UDP socket
	 */
	private final DatagramSocket udpSocket;
	
	/**
	 * The thread processing events received from the UDP socket
	 */
	private volatile Thread thread;
	
	
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
		this.udpPort=udpPort;
		
		this.udpSocket = new DatagramSocket(udpPort);
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
		System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		// The loop to get monitor from the socket 
		while (!terminateThread.get()) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				udpSocket.receive(packet);
			} catch (Exception e) {
				logger.error("Error receiving data from UDP socket",e);
				terminateThread.set(true);
				continue;
			}
			String receivedString = new String(packet.getData());
			logger.debug("Packet of size {} received from {}:{}: [{}]",
					packet.getData().length,
					packet.getAddress(),
					packet.getPort(),
					receivedString);
		}
		done.countDown();
		logger.debug("UDP loop thread terminated");
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
		logger.debug("Starting the UDP loop");
		thread = Plugin.getThreadFactory().newThread(this);
		thread.start();
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
		if (thread!=null) {
			thread.interrupt();
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
