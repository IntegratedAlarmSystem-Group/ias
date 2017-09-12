package org.eso.ias.converter.test.pluginconsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eso.ias.converter.pluginconsumer.RawDataReader;
import org.eso.ias.plugin.publisher.MonitorPointData;

/**
 * Testing facility that simulates the receiving of 
 * monitor point values and alarms.
 * <P>
 * Objects of this class simulate the production of monitor
 * point values and alarms by returning the values provided by the
 * user in the constructor or in the add method.
 * <BR>The ratio is to return such values when the test invokes
 * {@link #get(long, TimeUnit)} so it knows what values to expect
 * and can check for the correctness of the computation. 
 * <P>
 * This class is backed by a {@link ArrayBlockingQueue}:
 * the values are returned in a FIFO order and 
 * the max number of monitor points in the queue is limited to
 * {@link #MAX_QUEUE_CAPACITY}.
 * 
 * @author acaproni
 */
public class PluginSenderSimulator implements RawDataReader {
	
	/**
	 * The max number of items in the queue
	 */
	private final int MAX_QUEUE_CAPACITY = 100000;
	
	/**
	 * The monitor point values and alarms to return
	 * when the caller invokes {@link #get(long, TimeUnit)}
	 */
	private final ArrayBlockingQueue<MonitorPointData> mpPoints =new ArrayBlockingQueue<>(MAX_QUEUE_CAPACITY);

	/**
	 * Constructor with a predefined set of monitor point
	 * values and alarms to be sumbmitted to the converter
	 * 
	 * @param mpPointsToReturn The monitor points to return
	 */
	public PluginSenderSimulator(List<MonitorPointData> mpPointsToReturn) {
		Objects.requireNonNull(mpPointsToReturn);
		mpPoints.clear();
		mpPoints.addAll(mpPointsToReturn);
	}
	
	/**
	 * Constructor without monitor point values to submit to the converter
	 * 
	 * @param mpPointsToReturn The monitor points to return
	 */
	public PluginSenderSimulator() {
		this(new ArrayList<MonitorPointData>());
	}
	
	/**
	 * Adds a monitor point value or alarm to the list of values
	 * to return top the caller when invoking {@link #get(long, TimeUnit)}
	 * 
	 * @param mpd The monitor point value to add to the queue
	 * @ return <code>true</code> if the value has been added to the queue
	 *          or <code>false</code> if the queue is full
	 */
	public boolean addMonitorPointToReceive(MonitorPointData mpd) {
		Objects.requireNonNull(mpd);
		return mpPoints.offer(mpd);
	}
	
	/**
	 * Return and remove from the queue the first monitor point 
	 * value of alarms.
	 * 
	 * @param timeout: how long to wait before giving up, in units of unit
	 * @param timeunit: a TimeUnit determining how to interpret the timeout parameter
	 * @return the monitor point value or <code>null</code>
	 *         if the specified waiting time elapses before an element is available
	 * @see RawDataReader#get(long, TimeUnit)
	 */
	@Override
	public MonitorPointData get(long timeout, TimeUnit timeunit) throws InterruptedException {
		if (timeout<=0) {
			throw new IllegalArgumentException("The timeout must be greater then 0");
		}
		return mpPoints.poll(timeout, timeunit);
	}
	
	/**
	 * The number of monitor point values and alarms in the queue
	 * waiting to be processed
	 * 
	 * @return The number of monitor point values and alarms in the queue
	 */
	public int size() {
		return mpPoints.size();
	}

}
