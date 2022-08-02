package org.eso.ias.plugin.test.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eso.ias.plugin.ValueToSend;
import org.eso.ias.plugin.publisher.BufferedPublisherBase;
import org.eso.ias.plugin.publisher.MonitorPointDataToBuffer;
import org.eso.ias.plugin.publisher.MonitorPointSender;
import org.eso.ias.plugin.publisher.PublisherException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>BufferedPublisherStressTest</code> stress the {@link BufferedPublisherBase}
 * by sending a lot of {@link FilteredValue} and checking if
 * they are correctly sent to the {@link MonitorPointSender} implementation
 * to be sent to the IAS core.
 * <P>
 * This test is non deterministic because of the throttling.
 * <P>
 * This stress test is done by concurrently send {@link #totValuesToOffer}
 * filtered values some with the same ID, some other with different IDs.
 * The values are concurrently offered to the {@link BufferedPublisherBase} by {@link #numOfThreads}
 * threads (@see {@link ValuesProducerCallable#run()}).
 * <P>
 * The stress tests in <code>BufferedPublisherStressTest</code> are far behind the specification:
 * the IAS should be able to process 50000 monitor point per second while here 
 * we fire hundreds thousand of monitor points.
 *  
 * 
 * @author acaproni
 *
 */
public class BufferedPublisherStressTest extends PublisherTestCommon {
	
	/**
	 * The logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(BufferedPublisherStressTest.class);
	
	/**
	 * The number of threads concurrently pushing values in the publisher.
	 */
	private final int numOfThreads = 10;
	
	/**
	 * The executor service
	 */
	private final ExecutorService fixedThreadPoolExecutorSvc = Executors.newFixedThreadPool(numOfThreads,threadFactory);
	
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
		List<List<ValueToSend>> listsOfValues = new ArrayList<>();
		for (int t=0; t<numOfThreads; t++) {
			listsOfValues.add(generateValuesToSend(numOfValuesForEachThread, "ID"+t+"-", false,numOfValuesForEachThread*t , 1+t));
		}
		
		// Start all the threads
		List<Future<Integer>> results = new LinkedList<>();
		for (List<ValueToSend> values: listsOfValues) {
			Callable<Integer> callable = new ValuesProducerCallable(bufferedPublisher,values,publishedValues);
			Future<Integer> future = fixedThreadPoolExecutorSvc.submit(callable);
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
	    assertTrue(expectedValues.await(1,TimeUnit.MINUTES),"Too slow processing events");
	    
	    // Check if all values have been pushed
	    assertEquals(numOfValuesForEachThread*numOfThreads,sum);
	    // Check if all values have been published
	    assertEquals(numOfValuesForEachThread*numOfThreads,publishedValues.size());
	    
	    // Check if all the pushed IDs have been published
	    for (List<ValueToSend> values: listsOfValues) {
	    	for (ValueToSend pushedValue: values) {
	    		MonitorPointDataToBuffer d = receivedValuesFromBufferedPub.get(pushedValue.id);
	    		assertNotNull(d);
	    	}
	    }
	}
}
