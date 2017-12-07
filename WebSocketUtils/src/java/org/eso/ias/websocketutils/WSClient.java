package org.eso.ias.websocketutils;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSClient {

	/**
	 * Web socket client
	 */
	WebSocketClient client = new WebSocketClient();

	/**
	 * Topic defined to send messages to the IAS Core to the IAS Web Server
	 */
	String KafkaTopic = "test";

	/**
	 * Custom socket
	 */
	WebSocketSender socket = new WebSocketSender(this.KafkaTopic);

	/**
	 * Web Server URI
	 */
	String webserverUri = "ws://localhost:8000/core/";

	URI uri;

	ClientUpgradeRequest request = new ClientUpgradeRequest();

	/**
	 * WebSocket session required to send messages to the Web server
	 */
	Session session;

	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(WSClient.class);


	/**
	 * Initializes the WebSocket
	 */
	public void run() {
		try {
			this.uri = new URI(this.webserverUri);
			this.client.start();
		    this.client.connect(this.socket, this.uri, this.request);
		    logger.info("Connecting to : " + uri.toString());
		    while(this.socket.session==null) {
		    	TimeUnit.MILLISECONDS.sleep(100);
		    }
		}
		catch( Exception e) {
			logger.error("Error on WebSocket connection");
		}
	}


	public static void main(String[] args) throws Exception {

		WSClient ws = new WSClient();
		ws.run();

	}

}
