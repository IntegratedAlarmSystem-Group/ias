package org.eso.ias.plugin.test.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.eso.ias.plugin.filter.FilteredValue;
import org.eso.ias.plugin.publisher.MonitorPointData;
import org.eso.ias.plugin.publisher.MonitorPointDataToBuffer;
import org.eso.ias.plugin.publisher.PublisherBase;
import org.eso.ias.plugin.publisher.PublisherException;
import org.eso.ias.plugin.thread.PluginThreadFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stress test for the {@link PublisherBase}
 * 
 * @author acaproni
 *
 */
public class PublisherStressTest extends PublisherTestCommon {

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
	private final ExecutorService fixedPoolExecutorSvc = Executors.newFixedThreadPool(numOfThreads, new PluginThreadFactory());
	
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
		unbufferedPublisher.setUp();
		unbufferedPublisher.startSending();
		
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
			Callable<Integer> callable = new ValuesProducerCallable(unbufferedPublisher,values,publishedValues);
			Future<Integer> future = fixedPoolExecutorSvc.submit(callable);
			results.add(future);
		}
		
		// Wait the termination of all the threads
		logger.info("Waiting termination of threads...");
		int sum = 0;
	    for (Future<Integer> future : results) {
	      sum += future.get();
	    }
	    logger.info("{} sent MonitorPointData by {} threads",sum,numOfThreads);
	    
	    // Give time to flush
	    // Now all the elements in the queue have been added to a BufferedMonitoredSystemData
	    // but could not yet been published
	    boolean isNotTimeout=expectedValues.await(1,TimeUnit.MINUTES);
		logger.info("Published messages={}, num of publish={}",unbufferedPublisher.getPublishedMessages(),numOfPublishInvocationInUnbufferedPub.get());
	    assertTrue(isNotTimeout,"Too slow processing events");
	    
	    
	    // Check if all values have been pushed
	    assertEquals(numOfValuesForEachThread*numOfThreads,sum);
	    // Check if all values have been published
	    assertEquals(numOfValuesForEachThread*numOfThreads,publishedValues.size());
	    
	    // Check if all the pushed IDs have been published
	    for (List<ValueToSend> values: listsOfValues) {
	    	for (ValueToSend pushedValue: values) {
	    		MonitorPointDataToBuffer d = receivedValuesFromUnbufferedPub.get(pushedValue.id);
	    		assertNotNull(d);
	    	}
	    }
	}
	
	/**
	 * Sends filtered values that have same IDs so not all the published 
	 * messages must be published and checking the result is more complex
	 * because of the throttling.
	 * <P>
	 * Generate a list of values where all the values in each list have the same ID but different values.
	 * So, at the end, in the map there is only one monitor point for each value with the value
	 * equal to the last value published. 
	 * 
	 * @throws PublisherException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@Test
	public void stressTestSameIDs() throws PublisherException, InterruptedException, ExecutionException {
		unbufferedPublisher.setUp();
		unbufferedPublisher.startSending();
		
		int numOfValuesForEachThread=5000;
		expectedValues = new CountDownLatch(numOfValuesForEachThread*numOfThreads);
		
		// Generate one list of filtered values (with the same IDs) for each thread
		List<List<ValueToSend>> listsOfValues = new ArrayList<>();
		for (int t=0; t<numOfThreads; t++) {
			// Each list contains a value with the same id but different values
			listsOfValues.add(generateValuesToSend(numOfValuesForEachThread, "ID"+t+"-", true,numOfValuesForEachThread*t , 1+t));
		}
		
		// Start all the threads
		List<Future<Integer>> results = new LinkedList<>();
		for (List<ValueToSend> values: listsOfValues) {
			Callable<Integer> callable = new ValuesProducerCallable(unbufferedPublisher,values,publishedValues);
			Future<Integer> future = fixedPoolExecutorSvc.submit(callable);
			results.add(future);
		}
		
		// Wait the termination of all the threads
		// We do not know how many monitor point values will be sent due to the throttilng
		// so.. just wait for awhile
		logger.info("Waiting termination of threads sending the mPoints...");
		int sum = 0;
	    for (Future<Integer> future : results) {
	      sum += future.get();
	    }
	    logger.info("{} sent MonitorPointData by {} threads",sum,numOfThreads);
	    
	    // Give time to flush
	    // Now all the elements in the queue have been added to a BufferedMonitoredSystemData
	    // but could not yet been published
	    logger.info("Giving threads time to terminate...");
	    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
		logger.info("Published messages={}, num of publish={}",unbufferedPublisher.getPublishedMessages(),numOfPublishInvocationInUnbufferedPub.get());
		
		for (int t=0; t<numOfThreads; t++) {
			String id="ID"+t+"-";
			MonitorPointData mpData = receivedValuesFromUnbufferedPub.get(id);
			assertNotNull(mpData);
			assertEquals(id, mpData.getId());
			assertEquals(pluginId, mpData.getPluginID());
			assertNotNull(mpData.getPublishTime());
			assertFalse(mpData.getPublishTime().isEmpty());
			List<ValueToSend> values = listsOfValues.get(t);
			Long value = (Long)values.get(values.size()-1).value;
			assertEquals(value.intValue(),Long.parseLong(mpData.getValue()));
		}
	}

}
