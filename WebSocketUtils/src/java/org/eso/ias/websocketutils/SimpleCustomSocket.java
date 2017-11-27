package org.eso.ias.websocketutils;

import java.util.concurrent.CountDownLatch;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket(maxTextMessageSize = 64 * 1024)
public class SimpleCustomSocket {
	
	/**
	 * Signal to prevent the web socket from closing before a set of pending operations are performed
	 */
	private final CountDownLatch closeLatch = new CountDownLatch(1);
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(SimpleCustomSocket.class);
	/**
	 * WebSocket session required to send messages to the Web server
	 */
	public Session session;
	
	/**
	 * Operations performed on connection close
	 * 
	 * @param statusCode
	 * @param reason
	 */
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
	   logger.info("WebSocket connection closed: %d - %s%n", statusCode, reason);
	   this.session = null;
	   this.closeLatch.countDown(); // trigger latch
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
	       session.getRemote().sendStringByFuture("{\"stream\": \"core\", \"payload\": {\"text\": \"Hello\"}}");
	   }
	   catch (Throwable t) {
	       logger.error("WebSocket couldn't send the message",t);
	   }
	}

}
