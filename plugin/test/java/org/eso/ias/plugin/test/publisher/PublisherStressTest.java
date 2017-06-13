package org.eso.ias.plugin.test.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointDataToBuffer;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>PublisherStressTest</code> stress the {@link BufferedPublisherBase}
 * by sending a lot of {@link FilteredValue} and checking if
 * they are correctly sent to the {@link MonitorPointSender} implementation
 * to be sent to the IAS core.
 * <P>
 * This test is non deterministic because of the throttling.
 * <P>
 * This stress test is done by concurrently send {@link #totValuesToOffer}
 * filtered values some with the same ID, some other with different IDs.
 * The values are concurrently offered to the {@link BufferedPublisherBase} by {@link #numOfThreads}
 * threads (@see {@link ValuesProducer#run()}).
 * <P>
 * The stress tests in <code>PublisherStressTest</code> are far behind the specification:
 * the IAS should be able to process 50000 monitor point per second while here 
 * we fire hundreds thousand of monitor points.
 *  
 * 
 * @author acaproni
 *
 */
public class PublisherStressTest extends PublisherTestCommon {
	
	/**
	 * The callable to concurrently push values in the publisher
	 * 
	 * @author acaproni
	 *
	 */
	protected class ValuesProducer implements Callable<Integer> {
		
		/**
		 * The list of values to publish
		 */
		private final List<FilteredValue> values;
		
		/**
		 * Constrcutor
		 * @param values The list of values to publish
		 */
		public ValuesProducer(List<FilteredValue> values) {
			this.values=values;
		}

		/**
		 * Push the values in the list into the publisher
		 * 
		 * @see Callable#call()
		 */
		@Override
		public Integer call() throws PublisherException {
			logger.info("Going to submit {} values",values.size());
			int published=0;
			for (FilteredValue fv : values) {
				publishedValues.put(fv.id, fv);
				bufferedPublisher.offer(Optional.of(fv));
				published++;
			}
			logger.info("Terminating: {} values pushed",published);
			return Integer.valueOf(published);
		}
		
	}
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(PublisherStressTest.class);
	
	/**
	 * The number of threads concurrently pushing values in the publisher.
	 */
	private final int numOfThreads = 10;
	
	/**
	 * The executor service
	 */
	private ExecutorService executorSvc = Executors.newFixedThreadPool(numOfThreads,PluginThreadFactory.getThreadFactory());
	
	/**
	 * Sends filtered values that have different IDs so the test passes if 
	 * all the values have been sent to IAS core (i.e. published)
	 * 
	 * @throws PublisherException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@Test
	public void stressTest() throws PublisherException, InterruptedException, ExecutionException {
		bufferedPublisher.setUp();
		bufferedPublisher.startSending();
		
		int numOfValuesForEachThread=10000;
		expectedValues = new CountDownLatch(numOfValuesForEachThread*numOfThreads);
		// Generate one list of filtered values for each thread
		List<List<FilteredValue>> listsOfValues = new ArrayList<>();
		for (int t=0; t<numOfThreads; t++) {
			listsOfValues.add(generateFileteredValues(numOfValuesForEachThread, "ID"+t+"-", false,numOfValuesForEachThread*t , 1+t));
		}
		
		// Start all the threads
		List<Future<Integer>> results = new LinkedList<>();
		for (List<FilteredValue> values: listsOfValues) {
			Callable<Integer> callable = new ValuesProducer(values);
			Future<Integer> future = executorSvc.submit(callable);
			results.add(future);
		}
		
		// Wait the termination of all the threads
		logger.info("Waiting termination of threads...");
		int sum = 0;
	    for (Future<Integer> future : results) {
	      sum += future.get();
	    }
	    logger.info("{} received MonitorPointData",sum);
	    
	    // Give time to flush
	    // Now all the elements in the queue have been added to a BufferedMonitoredSystemData
	    // but could not yet been published
	    assertTrue("Too slow processing events",expectedValues.await(1,TimeUnit.MINUTES));
	    
	    // Check if all values have been pushed
	    assertEquals(numOfValuesForEachThread*numOfThreads,sum);
	    // Check if all values have been published
	    assertEquals(numOfValuesForEachThread*numOfThreads,publishedValues.size());
	    
	    // Check if all the pushed IDs have been published
	    for (List<FilteredValue> values: listsOfValues) {
	    	for (FilteredValue pushedValue: values) {
	    		MonitorPointDataToBuffer d = receivedValuesFromBufferedPub.get(pushedValue.id);
	    		assertNotNull(d);
	    	}
	    }
	    
	    bufferedPublisher.tearDown();
	}
}
