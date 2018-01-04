package org.eso.ias.webserversender;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerSender {

	/**
	 * Identifier
	 */
	String id;

	/**
	 * Web socket client
	 */
	WebSocketClient client = new WebSocketClient();

	/**
	 * Topic defined to send messages to the IAS Core to the IAS Web Server
	 */
	String kafkaTopic;

	/**
	 * Custom socket
	 */
	KafkaWebSocketConnector connector;

	/**
	 * Web Server URI
	 */
	String webserverUri;

	URI uri;

	ClientUpgradeRequest request = new ClientUpgradeRequest();

	/**
	 * WebSocket session required to send messages to the Web server
	 */
	Session session;

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WebServerSender.class);

  	/**
	 * Constructor
	 *
	 * @param id Identifier of the WebServerSender
	 * @param kafkaTopic Topic defined to send messages to the IAS Core to the IAS Web Server
	 */
	public WebServerSender(String id, String kafkaTopic, String webserverUri) {
		this.id = id;
		this.kafkaTopic = kafkaTopic;
		this.webserverUri = webserverUri;
		this.connector = new KafkaWebSocketConnector(this.id, this.kafkaTopic);
	}

	/**
	 * Initializes the WebSocket
	 */
	public void run() {
		try {
			this.uri = new URI(this.webserverUri);
			this.client.start();
			this.client.connect(this.connector, this.uri, this.request);
			logger.info("Connecting to : " + uri.toString());
			while(this.connector.session==null) {
				TimeUnit.MILLISECONDS.sleep(1000);
			}
			logger.debug("Connection started!");
		}
		catch( Exception e) {
			logger.error("Error on WebSocket connection");
		}
	}

	public void stop() {
		try {
			this.client.stop();
			logger.debug("Connection stopped!");
		}
		catch( Exception e) {
			logger.error("Error on Websocket stop");
		}
	}

	public static void main(String[] args) throws Exception {

		WebServerSender ws = new WebServerSender("WebServerSender", "test", "ws://localhost:8000/core/");
		ws.run();

	}

}
