package org.eso.ias.plugin;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.eso.ias.plugin.filter.Filter;
import org.eso.ias.plugin.filter.FilterException;
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.filter.NoneFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>MonitoredValue</code> is a monitor point value or alarm red from the 
 * controlled system and sent to the IAS core after applying the filter.
 * <P>
 * The history of samples needed to apply the filter is part
 * of the filter itself because its management depends on the filter.
 * <BR>For example a filter that returns only the last received value needs to save 
 * a history with only that sample, but a filter that averages the values acquired 
 * in the last minute needs a longer history even if its refresh rate is
 * much shorter then the averaging period.   
 * <P>
 * The <code>MonitoredValue</code> main tasks are:
 * <UL>
 * 	<LI>receive the values of the monitor points of the monitored system and
 *      apply the filter to generated a new value to send to the IAS
 * 	<LI>if the value has not been sent to the IAS when the refresh time interval elapses,
 *      apply the filter to the history to generate a new valu.
 * </UL>
 * <P>The <code>MonitoredValue</code> sends the value to the IAS core 
 * (i.e. to the {@link ChangeValueListener}) immediately if the generated value changed
 * or after the time interval elapses by a timer task implemented by the {@link #run()}
 * method and scheduled by the {@link #schedExecutorSvc} executor service.
 * 
 * 
 * @author acaproni
 *
 */
public class MonitoredValue implements Runnable {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(MonitoredValue.class);
	
	/**
	 * The ID of the monitored value
	 */
	public final String id;
	
	/**
	 * The refresh rate (msec) of this monitored value:
	 * the value must be sent to the IAS core on change or before the
	 * refresh rate expires
	 */
	public final long refreshRate;
	
	/**
	 * The filter to apply to the acquired samples 
	 * before sending the value to the IAS core 
	 */
	private final Filter filter;
	
	/**
	 * The scheduled executor service.
	 * It is needed to get a signal when the refresh rate elapses.
	 */
	private final ScheduledExecutorService schedExecutorSvc;
	
	/**
	 * The listener of updates of the value of this monitored point
	 */
	private final ChangeValueListener listener;
	
	/**
	 * The last value sent to the IAS core 
	 * (i.e. to the {@link #listener}).
	 */
	private Optional<FilteredValue> lastSentValue = Optional.empty();
	
	/**
	 * The point in time when the last value has been
	 * sent to the IAS core
	 */
	private long lastSentTimestamp;
	
	/**
	 * Build a {@link MonitoredValue} with the passed filter
	 * @param id The identifier of the value
	 * @param refreshRate The refresh time interval
	 * @param filter The filter to apply to the samples
	 * @param executorSvc The executor to schedule the thread
	 * @param listener The listener of updates
	 */
	public MonitoredValue(
			String id, 
			long refreshRate, 
			Filter filter, 
			ScheduledExecutorService executorSvc,
			ChangeValueListener listener) {
		if (id==null || id.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid monitored value ID");
		}
		if (refreshRate<=0) {
			throw new IllegalArgumentException("Invalid refresh rate vale: "+refreshRate);
		}
		if (filter==null) {
			throw new IllegalArgumentException("The filter can't be null");
		}
		if (executorSvc==null) {
			throw new IllegalArgumentException("The executor service can't be null");
		}
		if (listener==null) {
			throw new IllegalArgumentException("The listener can't be null");
		}
		this.id=id.trim();
		this.refreshRate=refreshRate;
		this.filter = filter;
		this.schedExecutorSvc=executorSvc;
		this.listener=listener;
		logger.debug("Monitor point "+this.id+" created with a refresh rate of "+this.refreshRate+"ms");
	}

	/**
	 * Build a {@link MonitoredValue} with the default filter, {@link NoneFilter}
	 * 
	 * @param id The identifier of the value
	 * @param refreshRate The refresh time interval
	 * @param executorSvc The executor to schedule the thread
	 */
	public MonitoredValue(
			String id, 
			long refreshRate, 
			ScheduledExecutorService executorSvc,
			ChangeValueListener listener) {
		this(id,refreshRate, new NoneFilter(id),executorSvc,listener);
	}
	
	/**
	 * Get and return the value to send i.e. the value
	 * returned applying the {@link #filter} to the #history of samples.
	 * 
	 * @return The value to send
	 */
	public Optional<FilteredValue> getValueTosend() {
		return filter.apply();
	}
	
	/**
	 * Adds a new sample to this monitor point.
	 * 
	 * @param s The not-null sample to add to the monitored value
	 * @throws FilterException If the submitted sample caused an exception in the filter
	 */
	public void submitSample(Sample s) throws FilterException {
		notifyListener(filter.newSample(s));
	}
	
	/**
	 * Send the value to the listener that in turn will forward to the IAS core.
	 * 
	 * @param value The not <code>null</code> value to send to the IAS
	 */
	private void notifyListener(Optional<FilteredValue> value) {
		assert(value!=null);
		try {
			listener.monitoredValueUpdated(value);
			lastSentTimestamp=System.currentTimeMillis();
			lastSentValue=value;
		} catch (Throwable t) {
			// In case of error sending the value, we log the exception
			// but do nothing else as we want to be ready to try to send it again
			// later
			logger.error("Error notifying the listener of the "+id+" monitor point change", t);
		}
	}
	
	/**
	 * Reschedule the time task after sending a value to the listener.
	 * <P>
	 * The timer must be scheduled to send the value to the listener 
	 * at the latest when the refresh rate elapse. 
	 */
	private void rescheduleTimer() {
		
	}

	/**
	 * The timer task scheduled when the refresh time interval elapses.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
	}
}
