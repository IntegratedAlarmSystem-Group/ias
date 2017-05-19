package org.eso.ias.plugin.test.publisher;

import org.eso.ias.plugin.publisher.MonitoredSystemData;
import org.eso.ias.plugin.publisher.PublisherBase;

/**
 * The interface to be notified about events happening in the {@link PublisherBase}.
 * 
 * @author acaproni
 *
 */
public interface PublisherEventsListener {

	/**
	 * Method invoked when {@link PublisherBase} invokes setUp
	 */
	public void initialized();
	
	/**
	 * Method invoked when {@link PublisherBase} invokes tearDown
	 */
	public void closed();
	
	/**
	 * Invoked when the publisher sends data
	 * 
	 * @param data The data published
	 */
	public void dataReceived(MonitoredSystemData data);
}
