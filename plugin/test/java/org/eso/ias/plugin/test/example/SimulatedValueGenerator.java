package org.eso.ias.plugin.test.example;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The generator of values for the simulated monitor points
 * @author acaproni
 *
 */
public class SimulatedValueGenerator  implements Runnable {
	
	/**
	 * The min value produced by the simulator for a monitor point
	 */
	private final double min;
	
	/**
	 * The max value produced by the simulator for a monitor point
	 */
	private final double max;
	
	/**
	 * The max difference between the old value and the new one
	 * to let each change appear more real then a radom jump
	 * betwee values too far one from another.
	 */
	private final double maxDelta;
	
	/**
	 * The time interval at which the value is updated
	 */
	private final int refreshRate;
	
	/**
	 * The random generator
	 */
	private Random rnd = new Random(System.currentTimeMillis());
	
	/**
	 * The value of the monitor point
	 */
	private final AtomicReference<Double> actualValue = new AtomicReference<Double>(0.0);

	/**
	 * Constructor
	 * 
	 * @param min Min possible value
	 * @param max Max possible value
	 * @param delta Max delta between consecutive values
	 * @param refreshRate Refresh time interval
	 * @param unit The time unit of the refresh rate 
	 */
	public SimulatedValueGenerator(double min, double max, double delta, int refreshRate,TimeUnit unit) {
		this.min=min;
		this.max=max;
		maxDelta=delta;
		this.refreshRate=refreshRate;
		actualValue.set(min+(max-min)/2);
	}
	
	/**
	 * @return the actualValue
	 */
	public double getActualValue() {
		return actualValue.get();
	}
	
	/**
	 * The thread to update the value of the monitor point.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		double val = rnd.nextDouble()*maxDelta;
		double sign = (rnd.nextBoolean()) ? 1: -1;
		actualValue.set(Double.valueOf(actualValue.get()+sign*val));
	}

}
