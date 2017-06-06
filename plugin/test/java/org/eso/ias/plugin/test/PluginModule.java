package org.eso.ias.plugin.test;

import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher;
import org.eso.ias.plugin.publisher.impl.ListenerPublisher.PublisherEventsListener;

import com.google.inject.AbstractModule;

/**
 * Provides dependency injection for plugin tests.
 * 
 * @author acaproni
 *
 */
public class PluginModule extends AbstractModule {
	
	/**
	 * The listener of events produced by the publisher
	 */
	private final PublisherEventsListener listener;
	
	private final String pluginId;
	
	private final String serverName;
	
	private final int port;
	
	private final ScheduledExecutorService executorSvc;
	
	public PluginModule(
			PublisherEventsListener listener,
			String pluginId,
			String serverName,
			int port,
			ScheduledExecutorService executorSvc) {
		this.pluginId=pluginId;
		this.serverName=serverName;
		this.port=port;
		this.executorSvc=executorSvc;
		this.listener=listener;
	}

	@Override
	protected void configure() {
	}
	
	MonitorPointSender provideMonitorPointSender() {
		return new ListenerPublisher(pluginId, serverName, port, executorSvc, listener);
	}

}
