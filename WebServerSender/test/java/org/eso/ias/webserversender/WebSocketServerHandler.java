package org.eso.ias.webserversender;


import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eso.ias.webserversender.WebServerSender.WebServerSenderListener;

@WebSocket
public class WebSocketServerHandler extends WebSocketHandler {
	
	public interface WebSocketServerListener {
		public void stringEventSent(String event);
	}
	
	public static WebSocketServerListener listener;
	
	public static void setListener(WebSocketServerListener newListener) {
		listener = newListener;
	}
	
	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.register(WebSocketServerHandler.class);
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
	    System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
	}

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());
        try {
            session.getRemote().sendString("Hello");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
    	if (listener != null) {
        	listener.stringEventSent(message);
    	}
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8081);
        WebSocketHandler wsHandler = new WebSocketServerHandler();
        WebSocketServerListener listener = new WebSocketServerListener(){
        	public void stringEventSent(String event) {
        		System.out.println("Mensaje Recibido!:" + event);
        	};
        };
        WebSocketServerHandler.setListener(listener);

        server.setHandler(wsHandler);
        server.start();
        server.join();
    }
}
