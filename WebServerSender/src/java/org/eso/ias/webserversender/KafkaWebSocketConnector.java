package org.eso.ias.webserversender;

import java.util.concurrent.CountDownLatch;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eso.ias.kafkautils.KafkaHelper;
import org.eso.ias.kafkautils.SimpleStringConsumer;
import org.eso.ias.kafkautils.SimpleStringProducer;
import org.eso.ias.kafkautils.SimpleStringConsumer.KafkaConsumerListener;
import org.eso.ias.kafkautils.SimpleStringConsumer.StartPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class KafkaWebSocketConnector implements KafkaConsumerListener {

  /**
   * Identifier
   */
  String id;

	/**
	 * Signal to prevent the web socket from closing before a set of pending operations are performed
	 */
	private final CountDownLatch closeLatch = new CountDownLatch(1);
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(KafkaWebSocketConnector.class);
	/**
	 * WebSocket session required to send messages to the Web server
	 */
	public Session session;
	/**
	 * IAS Core Kafka Consumer to get messages from the Core
	 */
	SimpleStringConsumer consumer;

  /**
	 * Constructor
	 *
   * @param id Identifier of the KafkaWebSocketConnector
	 * @param kafkaTopic Topic defined to send messages to the IAS Core to the IAS Web Server
	 */
	public KafkaWebSocketConnector(String id, String kafkaTopic) {
    this.id = id;
		// this.consumer = new SimpleStringConsumer(SimpleStringProducer.DEFAULT_BOOTSTRAP_SERVERS, kafkaTopic, this.id, this);
		this.consumer = new SimpleStringConsumer(KafkaHelper.DEFAULT_BOOTSTRAP_BROKERS, kafkaTopic, this.id, this);
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
	   this.closeLatch.countDown(); // trigger latch
	   this.consumer.tearDown();
	   System.exit(0); // TODO: Add WebServer reconnection
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
	       this.consumer.startGettingEvents(StartPosition.END);
		  //  this.session.getRemote().sendStringByFuture( "{\"text\": \""+ "Hola" +"\"}" );
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
		}
		catch (Exception e){
			logger.error("Cannot send messages to the Web Server", e);
		}
	}

}
